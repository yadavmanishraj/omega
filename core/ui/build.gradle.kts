plugins {
    alias(libs.plugins.omega.android.library.compose)
}

android {
    namespace = "com.manishraj.saavnmusic.ui.components"
}

dependencies {
    api(projects.core.designsystem)
    api(projects.core.model)

    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    // BackHandler for the playlist picker's create-mode back path
    // (F-11) — the picker lives here, so the dependency does too.
    implementation(libs.androidx.activity.compose)
    implementation(libs.coil.compose)
    implementation(libs.androidx.palette.ktx)
}
