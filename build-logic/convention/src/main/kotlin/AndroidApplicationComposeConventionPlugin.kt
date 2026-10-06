import com.android.build.api.dsl.ApplicationExtension
import com.manishraj.saavnmusic.buildlogic.configureComposeDependencies
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType

/**
 * omega.android.application.compose — Compose for the :app module.
 */
class AndroidApplicationComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "omega.android.application")
            apply(plugin = "org.jetbrains.kotlin.plugin.compose")

            val extension = extensions.getByType<ApplicationExtension>()
            extension.buildFeatures.compose = true
            configureComposeDependencies()
        }
    }
}
