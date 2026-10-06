plugins {
    alias(libs.plugins.omega.android.library)
    alias(libs.plugins.omega.hilt)
}

android {
    namespace = "com.manishraj.saavnmusic.data.settings"
}

dependencies {
    api(projects.core.model)
    implementation(projects.core.common)

    implementation(libs.datastore)
    implementation(libs.kotlinx.coroutines)
}
