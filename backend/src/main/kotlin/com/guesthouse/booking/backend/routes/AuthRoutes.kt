package com.guesthouse.booking.backend.routes

import com.guesthouse.booking.backend.auth.JwtConfig
import com.guesthouse.booking.backend.auth.PasswordHasher
import com.guesthouse.booking.backend.auth.StaffPrincipal
import com.guesthouse.booking.backend.db.Staff
import com.guesthouse.booking.backend.db.StaffPropertyAssignments
import com.guesthouse.booking.backend.model.ErrorResponse
import com.guesthouse.booking.backend.model.LoginRequest
import com.guesthouse.booking.backend.model.LoginResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.authRoutes() {
    post("/api/auth/login") {
        val request = call.receive<LoginRequest>()
        val email = request.email.trim()
        if (email.isEmpty() || request.password.isEmpty()) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Email and password are required"))
            return@post
        }

        val loginResult = transaction {
            val row = Staff.selectAll().toList().filter { it[Staff.email] == email }.firstOrNull()
                ?: return@transaction LoginFailure("Invalid email or password")

            if (!row[Staff.isActive]) {
                return@transaction LoginFailure("This account has been deactivated")
            }
            val storedHash = row[Staff.passwordHash]
            if (!PasswordHasher.verify(request.password, storedHash)) {
                return@transaction LoginFailure("Invalid email or password")
            }

            val staffId = row[Staff.id].value
            if (PasswordHasher.needsUpgrade(storedHash)) {
                Staff.update({ Staff.id eq staffId }) {
                    it[passwordHash] = PasswordHasher.hash(request.password)
                }
            }
            val role = row[Staff.role]
            val assignedPropertyIds = if (role == "CHAIN_ADMIN") {
                emptyList()
            } else {
                StaffPropertyAssignments.selectAll().toList()
                    .filter { it[StaffPropertyAssignments.staffId].value == staffId }
                    .map { it[StaffPropertyAssignments.propertyId].value }
            }

            LoginSuccess(
                StaffPrincipal(
                    staffId = staffId,
                    email = row[Staff.email],
                    displayName = row[Staff.displayName],
                    role = role,
                    assignedPropertyIds = assignedPropertyIds
                )
            )
        }

        when (loginResult) {
            is LoginFailure -> call.respond(HttpStatusCode.Unauthorized, ErrorResponse(loginResult.message))
            is LoginSuccess -> {
                val p = loginResult.principal
                call.respond(
                    LoginResponse(
                        token = JwtConfig.generateToken(p),
                        staffId = p.staffId,
                        email = p.email,
                        displayName = p.displayName,
                        role = p.role,
                        assignedPropertyIds = p.assignedPropertyIds
                    )
                )
            }
        }
    }
}

private sealed interface LoginResult
private data class LoginSuccess(val principal: StaffPrincipal) : LoginResult
private data class LoginFailure(val message: String) : LoginResult
