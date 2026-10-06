import com.manishraj.saavnmusic.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

/**
 * omega.android.feature — everything a feature module needs (mirrors Now in
 * Android's AndroidFeatureImplConventionPlugin, adapted to Omega's
 * Navigation-Compose setup): library + compose + Hilt convention plugins,
 * plus pre-wired implementation dependencies on the shared core UI modules,
 * the domain models, and the lifecycle / navigation artifacts every feature
 * screen uses. Features add only their feature-specific core dependencies
 * (:core:data, :core:playback, ...) in their own build files.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "omega.android.library")
            apply(plugin = "omega.android.library.compose")
            apply(plugin = "omega.hilt")

            dependencies {
                "implementation"(project(":core:ui"))
                "implementation"(project(":core:designsystem"))
                "implementation"(project(":core:model"))
                "implementation"(project(":core:common"))

                "implementation"(libs.findLibrary("androidx-lifecycle-runtime").get())
                "implementation"(libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
                "implementation"(libs.findLibrary("hilt-navigation-compose").get())
                "implementation"(libs.findLibrary("navigation-compose").get())
            }
        }
    }
}
