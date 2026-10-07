plugins {
    alias(libs.plugins.omega.android.library)
    alias(libs.plugins.omega.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.manishraj.saavnmusic.playback"
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.common)
    implementation(projects.core.datastore)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.media3.ui)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
}
