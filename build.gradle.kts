// build.gradle.kts

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor) // This is your Ktor JVM Application plugin
    alias(libs.plugins.kotlin.plugin.serialization)
    // REMOVE THIS LINE (it conflicts with Ktor's internal bundling)
    // id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.example"
version = "0.0.1" // Your project version

application {
    // This line is correct and tells Ktor how to start your application
    mainClass = "io.ktor.server.netty.EngineMain"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.netty)
    implementation(libs.logback.classic)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
    implementation("org.jetbrains.exposed:exposed-core:0.51.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.51.1")
    implementation("org.postgresql:postgresql:42.7.3")
    implementation("io.ktor:ktor-client-core:3.1.3")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
}

