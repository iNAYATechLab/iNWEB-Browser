// iNWEB Browser — VPN core module (pure JVM).
//
// Structural validation of imported VPN configurations (Phase 8 design,
// docs/PHASE8-VPN-SECURITY-DESIGN.md §5): WireGuard-style INI parsing
// with strict field/CIDR/endpoint/key-length checks. Secrets are opaque
// to the core (SecretValue) and can never leak through toString.
// No cryptography happens here — the Android layer owns key handling.
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
