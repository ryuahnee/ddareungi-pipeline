package com.jakdang.api.routes

import com.jakdang.api.db.PostgresClient
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.useRoutes(db: PostgresClient) {
    routing {
        route("/api/use") {
            get("/daily")   { call.respond(db.useDailyAgg()) }
            get("/monthly") { call.respond(db.useMonthlyAgg()) }
        }
    }
}
