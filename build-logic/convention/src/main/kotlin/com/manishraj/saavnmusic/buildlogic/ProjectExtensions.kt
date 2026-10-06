package com.manishraj.saavnmusic.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/** The shared version catalog (`gradle/libs.versions.toml`), visible to convention plugins. */
val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

/** Pins every Kotlin compile task in this project to JVM 17 bytecode. */
internal fun Project.configureKotlinJvmTarget() {
    tasks.withType(KotlinCompile::class.java).configureEach {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
    }
}

/**
 * Compose dependency wiring shared by the library/application compose
 * convention plugins: the Compose BOM as a platform plus the tooling
 * artifacts (Now in Android pattern). Callers set
 * `buildFeatures.compose = true` on their own extension type and apply the
 * Compose compiler plugin themselves.
 */
internal fun Project.configureComposeDependencies() {
    dependencies {
        val bom = libs.findLibrary("compose-bom").get()
        "implementation"(platform(bom))
        "androidTestImplementation"(platform(bom))
        "implementation"(libs.findLibrary("compose-ui-tooling-preview").get())
        "debugImplementation"(libs.findLibrary("compose-ui-tooling").get())
    }
}
