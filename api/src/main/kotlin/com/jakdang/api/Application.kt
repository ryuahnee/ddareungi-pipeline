package com.jakdang.api

import com.fasterxml.jackson.databind.SerializationFeature
import com.jakdang.api.db.PostgresClient
import com.jakdang.api.routes.stationRoutes
import com.jakdang.api.routes.statsRoutes
import com.jakdang.api.routes.subwayRoutes
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*

fun main() {
    val db = PostgresClient(
        System.getenv("GRAFANA_DB_URL")      ?: error("GRAFANA_DB_URL 필수"),
        System.getenv("GRAFANA_DB_USER")     ?: error("GRAFANA_DB_USER 필수"),
        System.getenv("GRAFANA_DB_PASSWORD") ?: error("GRAFANA_DB_PASSWORD 필수")
    )
    embeddedServer(Netty, port = 8090) {
        install(ContentNegotiation) {
            jackson {
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                findAndRegisterModules()
            }
        }
        stationRoutes(db)
        statsRoutes(db)
        subwayRoutes(db)
    }.start(wait = true)
}
