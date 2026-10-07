plugins {
    alias(libs.plugins.omega.android.feature)
}

android {
    namespace = "com.manishraj.saavnmusic.feature.search"
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.core.download)

    // SearchViewModel enqueues downloads directly (row-menu Download).
    implementation(libs.work.runtime)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    // Test-only: construct the real MusicRepository around in-memory
    // fakes for the ViewModel tests (mirrors :feature:library).
    testImplementation(libs.okhttp)
    testImplementation(libs.datastore)
    testImplementation(libs.kotlinx.serialization.json)
}
