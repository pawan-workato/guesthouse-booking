package com.guesthouse.booking.backend

import com.guesthouse.booking.backend.auth.configureAuth
import com.guesthouse.booking.backend.db.DatabaseFactory
import com.guesthouse.booking.backend.model.ErrorResponse
import com.guesthouse.booking.backend.model.HealthResponse
import com.guesthouse.booking.backend.routes.authRoutes
import com.guesthouse.booking.backend.routes.dataRoutes
import com.guesthouse.booking.backend.seed.DatabaseSeeder
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module).start(wait = true)
}

fun Application.module() {
    DatabaseFactory.init()
    DatabaseSeeder.seedIfEmpty()

    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            }
        )
    }
    install(CallLogging)
    configureAuth()
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.application.environment.log.error("Unhandled error", cause)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse(cause.message ?: "Internal error"))
        }
    }

    routing {
        get("/health") {
            call.respond(HealthResponse("ok"))
        }
        authRoutes()
        authenticate("auth-jwt") {
            dataRoutes()
        }
    }
}
