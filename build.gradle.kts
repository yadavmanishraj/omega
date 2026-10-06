// Top-level build file. By listing all the plugins used throughout all
// subprojects here (apply false), the build script classpath stays the same
// for every project, so the build-logic convention plugins can apply the
// underlying plugins by id inside each module (Now in Android pattern).
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
}
