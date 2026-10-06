import com.android.build.api.dsl.LibraryExtension
import com.manishraj.saavnmusic.buildlogic.configureComposeDependencies
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType

/**
 * omega.android.library.compose — Compose for Android libraries: applies the
 * library plugin, the Compose compiler plugin, enables the compose build
 * feature and wires the Compose BOM + tooling dependencies.
 */
class AndroidLibraryComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "omega.android.library")
            apply(plugin = "org.jetbrains.kotlin.plugin.compose")

            val extension = extensions.getByType<LibraryExtension>()
            extension.buildFeatures.compose = true
            configureComposeDependencies()
        }
    }
}
