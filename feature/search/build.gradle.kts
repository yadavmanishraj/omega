plugins {
    alias(libs.plugins.omega.android.feature)
}

android {
    namespace = "com.manishraj.saavnmusic.feature.search"
}

dependencies {
    implementation(projects.core.data)
}
