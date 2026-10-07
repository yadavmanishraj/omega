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

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    // Test-only: construct the real MusicRepository around in-memory
    // fakes for the ViewModel tests.
    testImplementation(libs.okhttp)
    testImplementation(libs.datastore)
}
