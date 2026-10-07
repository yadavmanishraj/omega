plugins {
    alias(libs.plugins.omega.android.application.compose)
    alias(libs.plugins.omega.hilt)
}

android {
    namespace = "com.manishraj.saavnmusic"

    defaultConfig {
        applicationId = "com.manishraj.saavnmusic"
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

dependencies {
    implementation(projects.feature.home)
    implementation(projects.feature.search)
    implementation(projects.feature.library)
    implementation(projects.feature.settings)
    implementation(projects.feature.player)
    implementation(projects.feature.detail)

    implementation(projects.core.ui)
    implementation(projects.core.designsystem)
    implementation(projects.core.data)
    implementation(projects.core.model)
    implementation(projects.core.common)
    implementation(projects.core.playback)
    implementation(projects.core.download)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.navigation.compose)

    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)

    implementation(libs.hilt.work)
    // SaavnApplication implements androidx.work.Configuration.Provider
    // directly, so the WorkManager runtime must be on this module's own
    // compile classpath at the catalog version. Without this, :app silently
    // compiled against hilt-work's ancient transitive work-runtime 2.3.4,
    // whose Provider shape Kotlin 2.4 no longer accepts a property override
    // for (the runtime classpath already resolved 2.10.0 via :core:download).
    implementation(libs.work.runtime)

    testImplementation(libs.junit)
}
