plugins {
    alias(libs.plugins.omega.android.library.compose)
}

android {
    namespace = "com.manishraj.saavnmusic.ui.theme"
}

dependencies {
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)

    // ProvideReducedMotion re-reads the animator scale on ON_RESUME.
    implementation(libs.androidx.lifecycle.runtime)
}
