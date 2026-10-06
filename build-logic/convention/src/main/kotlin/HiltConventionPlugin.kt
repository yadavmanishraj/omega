import com.manishraj.saavnmusic.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

/**
 * omega.hilt — Hilt wiring (mirrors Now in Android's HiltConventionPlugin):
 * KSP + the Hilt compiler everywhere; `hilt-core` on plain JVM modules and
 * the Hilt Gradle plugin + `hilt-android` on Android modules.
 */
class HiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.google.devtools.ksp")

            dependencies {
                "ksp"(libs.findLibrary("hilt-compiler").get())
                "ksp"(libs.findLibrary("kotlin-metadata").get())
            }

            // Support for JVM modules, based on org.jetbrains.kotlin.jvm.
            pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
                dependencies {
                    "implementation"(libs.findLibrary("hilt-core").get())
                }
            }

            // Support for Android modules, based on com.android.base.
            pluginManager.withPlugin("com.android.base") {
                apply(plugin = "com.google.dagger.hilt.android")
                dependencies {
                    "implementation"(libs.findLibrary("hilt-android").get())
                }
            }
        }
    }
}
