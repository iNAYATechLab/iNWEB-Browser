// iNWEB Browser — Home / New Tab core module (pure JVM).
//
// The module consumes browser-shell, tracking-protection, and offline contracts.
// scripts/validate_kotlin_core.sh is the repository validation authority and must
// compile those dependency modules before this one (its --deps mechanism).

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
