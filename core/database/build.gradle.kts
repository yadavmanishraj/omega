plugins {
    alias(libs.plugins.omega.android.library)
    alias(libs.plugins.omega.hilt)
    alias(libs.plugins.omega.android.room)
}

android {
    namespace = "com.manishraj.saavnmusic.data.local"
}

dependencies {
    api(projects.core.model)
    implementation(projects.core.common)

    implementation(libs.kotlinx.coroutines)
}
