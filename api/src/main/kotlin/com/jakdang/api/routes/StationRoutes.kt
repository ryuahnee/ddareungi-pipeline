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
            get("/holiday-depletion") { call.respond(db.holidayDepletion()) }
            get("/morning-rush")      { call.respond(db.morningRush()) }
            get("/evening-rush")      { call.respond(db.eveningRush()) }
            get("/cluster")           { call.respond(db.stationCluster()) }
        }
    }
}
