package com.jakdang.api.routes

import com.jakdang.api.db.PostgresClient
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.subwayRoutes(db: PostgresClient) {
    routing {
        route("/api/subway") {
            get("/rush") {
                val hour = call.request.queryParameters["hour"]?.toIntOrNull()
                call.respond(db.subwayRushDepletion(hour))
            }
        }
    }
}
