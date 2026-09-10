package com.jakdang.api.routes

import com.jakdang.api.db.PostgresClient
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.statsRoutes(db: PostgresClient) {
    routing {
        route("/api/stats") {
            get("/weather-depletion") { call.respond(db.weatherDepletion()) }
        }
    }
}
