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
    implementation(libs.coil.compose)
    implementation(libs.androidx.palette.ktx)
}
