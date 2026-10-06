plugins {
    alias(libs.plugins.omega.android.feature)
}

android {
    namespace = "com.manishraj.saavnmusic.feature.library"
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.core.download)

    // LibraryViewModel enqueues downloads directly (retry / undo-delete).
    implementation(libs.work.runtime)
}
