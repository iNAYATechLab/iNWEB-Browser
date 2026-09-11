// iNWEB Browser — profiles core module (pure JVM).
//
// The profile model behind MASTER-SPEC §28 (Phase 9 design,
// docs/PHASE9-PROFILES-SYNC-DESIGN.md §1): profile records, active-profile
// selection, and per-profile store routing with namespace isolation.
// Compiles and tests without any Android dependency; the ui/ patch (0022)
// binds namespaces to real per-profile data directories (B-001).
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
