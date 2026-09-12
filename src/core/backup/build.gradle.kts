// iNWEB Browser — backup core module (pure JVM).
//
// The versioned backup bundle behind MASTER-SPEC §32 (Phase 9 design,
// docs/PHASE9-PROFILES-SYNC-DESIGN.md §5): manifest, per-entry SHA-256
// checksums, forward-only format gating, restore preview, and per-entry
// corruption tolerance. Encryption happens at the Android layer
// (Keystore envelope, ADR-025) — this core contains no secret handling
// and no cryptography beyond the platform SHA-256 integrity primitive.
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
