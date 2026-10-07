plugins {
    alias(libs.plugins.omega.android.feature)
}

android {
    namespace = "com.manishraj.saavnmusic.feature.player"
}

dependencies {
    implementation(projects.core.playback)
    implementation(projects.core.data)
    implementation(projects.core.download)

    implementation(libs.work.runtime)

    testImplementation(libs.junit)
}
