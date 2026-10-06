import com.manishraj.saavnmusic.buildlogic.configureKotlinJvmTarget
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

/**
 * omega.jvm.library — plain Kotlin/JVM library targeting JVM 17
 * (:core:model, :core:common).
 */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "java-library")
            apply(plugin = "org.jetbrains.kotlin.jvm")

            extensions.configure<JavaPluginExtension> {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }

            configureKotlinJvmTarget()
        }
    }
}
