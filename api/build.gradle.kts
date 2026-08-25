plugins {
    kotlin("jvm")
    application
}

application {
    mainClass.set("com.jakdang.api.ApplicationKt")
}

val ktorVersion = "2.3.12"

dependencies {
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.2")
    implementation("org.postgresql:postgresql:42.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("ch.qos.logback:logback-classic:1.4.14")
}
