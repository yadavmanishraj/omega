plugins {
    alias(libs.plugins.omega.android.feature)
}

android {
    namespace = "com.manishraj.saavnmusic.feature.detail"
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.core.download)

    // DetailViewModel enqueues downloads directly (row-menu Download).
    implementation(libs.work.runtime)
}
