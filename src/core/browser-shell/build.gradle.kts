// iNWEB Browser — core browser-shell module (pure JVM).
//
// This module contains the engine-independent shell logic (tab state,
// omnibox parsing, session persistence, download state machine, settings
// model). It compiles and tests without any Android dependency, which is
// what allows continuous validation in CI and in the authoring sandbox.
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
