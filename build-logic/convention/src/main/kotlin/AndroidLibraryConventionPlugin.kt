import com.android.build.api.dsl.LibraryExtension
import com.manishraj.saavnmusic.buildlogic.configureKotlinJvmTarget
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

/**
 * omega.android.library — base Android library configuration for Omega
 * (mirrors Now in Android's AndroidLibraryConventionPlugin): compileSdk 37
 * (required by Compose 1.12 / BOM 2026.09.00),
 * minSdk 26, JVM 17, the standard test runner, and a resource prefix derived
 * from the module path (":core:network" -> "core_network_").
 */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.library")
            apply(plugin = "org.jetbrains.kotlin.android")

            extensions.configure<LibraryExtension> {
                compileSdk = 37

                defaultConfig {
                    minSdk = 26
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }

                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }

                // The resource prefix is derived from the module path, so
                // resources inside ":core:network" must be prefixed with
                // "core_network_" (Now in Android pattern).
                resourcePrefix =
                    path
                        .split("""\W""".toRegex())
                        .drop(1)
                        .distinct()
                        .joinToString(separator = "_")
                        .lowercase() + "_"
            }

            configureKotlinJvmTarget()
        }
    }
}
