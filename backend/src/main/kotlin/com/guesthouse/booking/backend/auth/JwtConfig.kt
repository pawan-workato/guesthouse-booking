package com.guesthouse.booking.backend.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.Payload
import com.auth0.jwt.interfaces.JWTVerifier
import io.ktor.server.application.Application
import io.ktor.server.auth.Principal
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTCredential
import io.ktor.server.auth.jwt.jwt
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.util.Date

data class StaffPrincipal(
    val staffId: Long,
    val email: String,
    val displayName: String,
    val role: String,
    val assignedPropertyIds: List<Long>
) : Principal

object JwtConfig {
    private const val CLAIM_STAFF_ID = "staffId"
    private const val CLAIM_DISPLAY_NAME = "displayName"
    private const val CLAIM_ROLE = "role"
    private const val CLAIM_PROPERTY_IDS = "propertyIds"

    private val json = Json { ignoreUnknownKeys = true }

    fun secret(): String = System.getenv("JWT_SECRET") ?: "dev-secret-change-me-in-production"

    fun issuer(): String = System.getenv("JWT_ISSUER") ?: "guesthouse-booking"

    fun audience(): String = System.getenv("JWT_AUDIENCE") ?: "guesthouse-staff"

    fun tokenLifetimeMs(): Long = 24L * 60 * 60 * 1000

    fun makeVerifier(): JWTVerifier =
        JWT.require(Algorithm.HMAC256(secret()))
            .withAudience(audience())
            .withIssuer(issuer())
            .build()

    fun generateToken(principal: StaffPrincipal): String {
        val now = System.currentTimeMillis()
        val propertyIdsJson = json.encodeToString(
            ListSerializer(Long.serializer()),
            principal.assignedPropertyIds
        )
        return JWT.create()
            .withIssuer(issuer())
            .withAudience(audience())
            .withSubject(principal.email)
            .withClaim(CLAIM_STAFF_ID, principal.staffId)
            .withClaim(CLAIM_DISPLAY_NAME, principal.displayName)
            .withClaim(CLAIM_ROLE, principal.role)
            .withClaim(CLAIM_PROPERTY_IDS, propertyIdsJson)
            .withIssuedAt(Date(now))
            .withExpiresAt(Date(now + tokenLifetimeMs()))
            .sign(Algorithm.HMAC256(secret()))
    }

    fun principalFrom(credential: JWTCredential): StaffPrincipal? {
        val payload: Payload = credential.payload
        val staffId = payload.getClaim(CLAIM_STAFF_ID).asLong() ?: return null
        val email = payload.subject ?: return null
        val displayName = payload.getClaim(CLAIM_DISPLAY_NAME).asString() ?: return null
        val role = payload.getClaim(CLAIM_ROLE).asString() ?: return null
        val propertyIdsJson = payload.getClaim(CLAIM_PROPERTY_IDS).asString() ?: "[]"
        val propertyIds = runCatching {
            json.decodeFromString<List<Long>>(propertyIdsJson)
        }.getOrDefault(emptyList())
        return StaffPrincipal(staffId, email, displayName, role, propertyIds)
    }
}

fun Application.configureAuth() {
    authentication {
        jwt("auth-jwt") {
            verifier(JwtConfig.makeVerifier())
            validate { credential -> JwtConfig.principalFrom(credential) }
        }
    }
}
