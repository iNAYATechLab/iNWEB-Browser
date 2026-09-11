// iNWEB Browser — tracking-protection core module (pure JVM).
//
// The engine-independent heart of the Privacy Engine (§10) and the Ad
// Blocker (§11): filter-list parsing, network-request matching, per-site
// allowlisting, and decision statistics. It compiles and tests without any
// Android dependency (ADR-009).
//
// Toolchain note: the Kotlin version here must match the pinned version in
// scripts/validate_kotlin_core.sh (that script compiles without Gradle).

plugins {
    kotlin("jvm") version "2.4.20"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnit()
}
