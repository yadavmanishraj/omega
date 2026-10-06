plugins {
    alias(libs.plugins.omega.android.library)
    alias(libs.plugins.omega.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.manishraj.saavnmusic.data.remote"
}

dependencies {
    api(projects.core.model)
    api(projects.core.common)

    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.kotlinx.coroutines.test)
}
