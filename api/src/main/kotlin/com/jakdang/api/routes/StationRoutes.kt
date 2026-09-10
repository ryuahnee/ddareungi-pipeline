package com.jakdang.api.routes

import com.jakdang.api.db.PostgresClient
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.stationRoutes(db: PostgresClient) {
    routing {
        route("/api/stations") {
            get("/snapshot")          { call.respond(db.stationSnapshot()) }
            get("/depletion")         { call.respond(db.depletionAlert()) }
            get("/congestion")        { call.respond(db.congestionAlert()) }
            get("/depletion-weather") { call.respond(db.depletionWithWeather()) }
            get("/version-usage")     { call.respond(db.stationVersionUsage()) }
        }
    }
}
