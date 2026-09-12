// iNWEB Browser — sync core module (pure JVM).
//
// The transport-agnostic sync queue model behind MASTER-SPEC §30 (Phase 9
// design, docs/PHASE9-PROFILES-SYNC-DESIGN.md §3): append-only change log
// with monotonic revisions, retry/backoff, last-writer-wins conflict
// resolution with tombstones. FUTURE INFRASTRUCTURE ONLY (§29): no sync
// backend exists, nothing here is a user-facing feature, and no UI may
// claim otherwise.
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
