plugins {
    alias(libs.plugins.omega.android.library)
    alias(libs.plugins.omega.hilt)
}

android {
    namespace = "com.manishraj.saavnmusic.data.repository"
}

dependencies {
    api(projects.core.model)
    api(projects.core.common)
    api(projects.core.network)
    api(projects.core.database)
    api(projects.core.datastore)

    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    // Test-only: construct the real JioSaavnClient / SettingsRepository
    // around in-memory fakes (the local-library tests never call them).
    testImplementation(libs.okhttp)
    testImplementation(libs.datastore)
}
