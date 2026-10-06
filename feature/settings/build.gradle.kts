plugins {
    alias(libs.plugins.omega.android.feature)
}

android {
    namespace = "com.manishraj.saavnmusic.feature.settings"
}

dependencies {
    implementation(projects.core.data)
}
