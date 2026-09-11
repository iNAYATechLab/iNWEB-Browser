// iNWEB Browser — offline core module (pure JVM).
//
// The offline library registry behind MASTER-SPEC §17 (Phase 7 design,
// docs/PHASE7-OFFLINE-DESIGN.md §2/§5): saved-page records with real byte
// sizes, quota accounting, LRU eviction, and a persistence seam. Compiles
// and tests without any Android dependency; the offline/ patches (0017+)
// bind it to the MHTML snapshot pipeline (B-001).
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
