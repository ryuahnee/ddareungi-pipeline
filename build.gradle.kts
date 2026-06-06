plugins {
    kotlin("jvm") version "1.9.25" apply false
    kotlin("plugin.serialization") version "1.9.25" apply false
}

subprojects {
    group = "com.jakdang"
    version = "0.0.1"

    repositories {
        mavenCentral()
    }
}
