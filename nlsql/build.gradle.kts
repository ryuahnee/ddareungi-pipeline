plugins {
    kotlin("jvm")
    application
}

application {
    mainClass.set("com.jakdang.nlsql.ApplicationKt")
}

dependencies {
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2")
    implementation("org.postgresql:postgresql:42.7.3")
    implementation("com.github.jsqlparser:jsqlparser:4.9")
    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.3")
}

tasks.test {
    useJUnitPlatform()
}
