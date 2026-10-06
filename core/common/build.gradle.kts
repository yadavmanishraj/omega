plugins {
    alias(libs.plugins.omega.jvm.library)
    alias(libs.plugins.omega.hilt)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
}
