import org.jetbrains.compose.desktop.application.dsl.TargetFormat
plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    kotlin("plugin.serialization")
}

// CI passes -PappVersion=<tag> (see .github/workflows/release.yml); local builds use the fallback.
val appVersion = (project.findProperty("appVersion") as String?) ?: "9.0.3"

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

    implementation("io.ktor:ktor-client-cio:3.6.0")
    implementation(compose.components.resources)
    implementation(compose.material3)
    implementation(compose.ui)
    implementation(compose.material)
    // Compose 1.8+ no longer bundles the icons; 1.7.3 is the final published version of the core set
    implementation("org.jetbrains.compose.material:material-icons-core:1.7.3")

    implementation("org.slf4j:slf4j-api:2.0.19") // SLF4J API
    implementation("ch.qos.logback:logback-classic:1.6.3")

    implementation("io.insert-koin:koin-compose:4.2.2")

    implementation("io.ktor:ktor-client-content-negotiation:3.6.0")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.6.0")
    // Kotlin serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    implementation("org.json:json:20260814")
    implementation("com.github.junrar:junrar:8.1.1")

    testImplementation(kotlin("test"))
    // Gradle 9 no longer auto-supplies the JUnit launcher
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

// Bakes the app version into the code (buildinfo.BuildInfo.VERSION) so the About dialog can show it
// and compare it with the latest GitHub release, both in `./gradlew run` and in the installed EXE.
val generateBuildInfo by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/buildinfo")
    val version = appVersion
    inputs.property("version", version)
    outputs.dir(outputDir)
    doLast {
        val file = outputDir.get().file("buildinfo/BuildInfo.kt").asFile
        file.parentFile.mkdirs()
        file.writeText("package buildinfo\n\nobject BuildInfo {\n    const val VERSION = \"$version\"\n}\n")
    }
}
kotlin.sourceSets["main"].kotlin.srcDir(generateBuildInfo)

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Exe)
            // Runtime modules Compose's `suggestRuntimeModules` reports as referenced by the dependencies
            // (the trimmed jlink image otherwise omits them). A real Ktor request works without them in
            // testing, so this is precautionary, not a fix for an observed failure.
            modules("java.instrument", "java.management", "jdk.unsupported")
            packageName = "Fast Localizer"
            packageVersion = appVersion
            windows {
                perUserInstall = true  // Ensures the app is installed per user, not system-wide
                shortcut = true
                menuGroup = "Fast Localizer"
            }
        }
    }
}

compose.resources {
    publicResClass = true
    generateResClass = always
}
