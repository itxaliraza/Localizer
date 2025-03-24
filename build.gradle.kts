import org.jetbrains.compose.desktop.application.dsl.TargetFormat
plugins {
    kotlin("jvm")
    id("org.jetbrains.compose") version "1.8.0-beta01"
    id("org.jetbrains.kotlin.plugin.compose")
    kotlin("plugin.serialization") version "2.0.0"

}

group = "com.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).
    // With compose.desktop.common you will also lose @Preview functionality
    implementation(compose.desktop.currentOs)

    implementation("io.ktor:ktor-client-cio:2.3.12")
    implementation(compose.components.resources)
    implementation(compose.material3)
    implementation(compose.ui)
    implementation(compose.material)

    implementation("org.slf4j:slf4j-api:2.0.9") // SLF4J API
    implementation("ch.qos.logback:logback-classic:1.4.11")

    implementation("io.insert-koin:koin-compose:4.0.0-RC2")

    implementation("io.ktor:ktor-client-content-negotiation:2.3.4")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.4")
    // Kotlin serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    implementation("org.json:json:20210307")
    implementation("com.github.junrar:junrar:7.5.5")

}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Exe)
            packageName = "Localizer"
            packageVersion = "2.0.0"
            windows {
                perUserInstall = true  // Ensures the app is installed per user, not system-wide
            }
        }
    }
}

compose.resources {
    publicResClass = true
    generateResClass = always
}
