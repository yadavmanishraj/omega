plugins {
    alias(libs.plugins.omega.android.library)
    alias(libs.plugins.omega.hilt)
}

android {
    namespace = "com.manishraj.saavnmusic.playback"
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.common)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.media3.ui)
    implementation(libs.kotlinx.coroutines)
}
