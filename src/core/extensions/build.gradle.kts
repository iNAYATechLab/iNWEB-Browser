// iNWEB Browser — extensions core module (pure JVM).
//
// The management model behind MASTER-SPEC §16 (Phase 6 design,
// docs/PHASE6-EXTENSION-DESIGN.md §4): install/enable/disable/remove
// state machine, sideload-update version comparison, and
// permission-review records. It compiles and tests without any Android
// dependency; the Chromium patches bind this model to the real
// extension runtime (B-001).
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
