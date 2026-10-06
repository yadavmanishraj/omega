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

    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization)
    implementation(libs.kotlinx.coroutines)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
