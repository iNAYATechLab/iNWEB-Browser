// iNWEB Browser — notifications core module (pure JVM).
//
// The §33 notification policy model behind the Phase 11 design
// (docs/PHASE11-NOTIFICATIONS-FEATURES-DESIGN.md §1, ADR-028):
// the auditable channel/event registry, per-channel user toggles,
// real-event-only decisions, and the lazy POST_NOTIFICATIONS
// permission state machine.
// Compiles and tests without any Android dependency; the settings/
// + ui/ patch bindings create the real channels and fire real events
// (B-001).
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
