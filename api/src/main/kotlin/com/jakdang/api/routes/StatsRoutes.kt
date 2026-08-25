package com.jakdang.api.routes

import com.jakdang.api.db.PostgresClient
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.statsRoutes(db: PostgresClient) {
    routing {
        route("/api/stats") {
            get("/weather")           { call.respond(db.weatherBikeStats()) }
            get("/weather-depletion") { call.respond(db.weatherDepletion()) }
            get("/hourly-weather")    { call.respond(db.hourlyWeatherBike()) }
            get("/holiday")           { call.respond(db.holidayBikeStats()) }
            get("/bike-movement")     { call.respond(db.bikeMovement()) }
            get("/weather-threshold") { call.respond(db.weatherThreshold()) }
            get("/cluster-profile")   { call.respond(db.clusterProfile()) }
        }
    }
}
