plugins {
    alias(libs.plugins.omega.android.feature)
}

android {
    namespace = "com.manishraj.saavnmusic.feature.search"
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.core.download)

    // SearchViewModel enqueues downloads directly (row-menu Download).
    implementation(libs.work.runtime)
}
