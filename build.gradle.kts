plugins {
    kotlin("jvm") version "1.9.0"
    application
}

group = "cerdcee.secretsanta"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}

dependencies {
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2") {
        because("read json config to objects")
    }
    implementation("com.github.UnitTestBot.kosat:kosat:main-SNAPSHOT") {
        because("SAT-solver")
    }
    implementation("com.github.spullara.mustache.java:compiler:0.9.6") {
        because("email templates")
    }
    implementation("org.apache.commons:commons-email:1.5") {
        because("email")
    }

    testImplementation(kotlin("test"))
    testImplementation("io.strikt:strikt-core:0.34.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}

application {
    mainClass.set("MainKt")
}