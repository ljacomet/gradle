import org.jetbrains.kotlin.gradle.plugin.KaptExtension

plugins {
    kotlin("jvm") version "1.9.24"
    kotlin("kapt") version "1.9.24"
}

repositories {
    mavenCentral()
}

// tag::cacheKapt[]
pluginManager.withPlugin("kotlin-kapt") {
    configure<KaptExtension> { useBuildCache = true }
}
// end::cacheKapt[]

// tag::fabricKotlin[]
apply(from = "fabric.gradle")
// end::fabricKotlin[]
