plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
}

application {
    mainClass.set("com.jakdang.batch.MainKt")
}

val ktorVersion = "2.3.12"

dependencies {
    // HTTP 클라이언트
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    // JSON
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // DuckDB
    implementation("org.duckdb:duckdb_jdbc:1.1.3")

    // PostgreSQL
    implementation("org.postgresql:postgresql:42.7.3")

    // 로깅
    implementation("ch.qos.logback:logback-classic:1.4.14")

    // 코루틴
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
}

tasks.shadowJar {
    archiveBaseName.set("ddareungi-batch")
    archiveClassifier.set("")
    archiveVersion.set("")
}
