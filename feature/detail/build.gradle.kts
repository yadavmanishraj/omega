plugins {
    alias(libs.plugins.omega.android.feature)
}

android {
    namespace = "com.manishraj.saavnmusic.feature.detail"
}

dependencies {
    implementation(projects.core.data)
}
