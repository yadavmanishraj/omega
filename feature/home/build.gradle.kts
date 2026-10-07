plugins {
    alias(libs.plugins.omega.android.feature)
}

android {
    namespace = "com.manishraj.saavnmusic.feature.home"
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.core.download)
    implementation(projects.core.playback)

    // HomeViewModel enqueues downloads directly (row-menu Download).
    implementation(libs.work.runtime)

    testImplementation(libs.junit)
}
