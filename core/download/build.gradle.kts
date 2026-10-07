plugins {
    alias(libs.plugins.omega.android.library)
    alias(libs.plugins.omega.hilt)
}

android {
    namespace = "com.manishraj.saavnmusic.download"
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.common)
    implementation(projects.core.data)

    implementation(libs.work.runtime)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines)

    testImplementation(libs.junit)
}
