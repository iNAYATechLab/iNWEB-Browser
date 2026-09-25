// iNWEB Browser — approved popular-site catalogue (pure JVM, Lead-owned).
//
// The admission set behind the Home rail: only catalogue rows are shown,
// each with its reviewed destination name and accessibility label. The
// catalogue ships empty, which is what keeps the section hidden until
// product and security approve a row.
//
// Toolchain note: the Kotlin version here must match the pinned version in
// scripts/validate_kotlin_core.sh (that script compiles without Gradle).
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
