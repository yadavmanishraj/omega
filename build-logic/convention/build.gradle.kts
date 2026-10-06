import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "com.manishraj.saavnmusic.buildlogic"

// Configure the build-logic plugins to target JDK 17. This matches the JDK
// used to build the project and is unrelated to what runs on device.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.room.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidLibrary") {
            id =
                libs.plugins.omega.android.library
                    .get()
                    .pluginId
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidLibraryCompose") {
            id =
                libs.plugins.omega.android.library.compose
                    .get()
                    .pluginId
            implementationClass = "AndroidLibraryComposeConventionPlugin"
        }
        register("androidApplication") {
            id =
                libs.plugins.omega.android.application
                    .get()
                    .pluginId
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidApplicationCompose") {
            id =
                libs.plugins.omega.android.application.compose
                    .get()
                    .pluginId
            implementationClass = "AndroidApplicationComposeConventionPlugin"
        }
        register("androidFeature") {
            id =
                libs.plugins.omega.android.feature
                    .get()
                    .pluginId
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("hilt") {
            id =
                libs.plugins.omega.hilt
                    .get()
                    .pluginId
            implementationClass = "HiltConventionPlugin"
        }
        register("jvmLibrary") {
            id =
                libs.plugins.omega.jvm.library
                    .get()
                    .pluginId
            implementationClass = "JvmLibraryConventionPlugin"
        }
        register("androidRoom") {
            id =
                libs.plugins.omega.android.room
                    .get()
                    .pluginId
            implementationClass = "AndroidRoomConventionPlugin"
        }
    }
}
