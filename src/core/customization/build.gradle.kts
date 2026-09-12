// iNWEB Browser — customization core module (pure JVM).
//
// The toolbar-configuration model behind MASTER-SPEC §23 (Phase 11
// design, docs/PHASE11-NOTIFICATIONS-FEATURES-DESIGN.md §3): the
// user-reorderable/hideable bottom-bar item set with strict
// validation and a persistence seam.
// Compiles and tests without any Android dependency; the ui/ patch
// binds the model to the real bar and stores the config in
// preferences (B-001).
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
