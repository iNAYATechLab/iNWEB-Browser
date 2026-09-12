// iNWEB Browser — clear-data core module (pure JVM).
//
// The clear-browsing-data plan model behind the Phase 12 hardening
// design item 2 (docs/PHASE12-PRODUCTION-HARDENING-DESIGN.md §3):
// the item universe driven by docs/STORAGE-INVENTORY.yaml's clear
// column, per-item dry-run previews, and execution through REAL store
// APIs (browser-shell, tracking-protection, offline modules).
//
// NOTE: this module has cross-module classpath dependencies —
// scripts/validate_kotlin_core.sh compiles it with browser-shell,
// tracking-protection, and offline on the classpath (that script is
// the single validation authority; a standalone Gradle build of this
// module would need composite-build wiring, ADR-012 lineage).
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
