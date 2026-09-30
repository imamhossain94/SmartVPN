// Top-level build file. Plugins are declared here but not applied so that each
// module can opt in via the `plugins { alias(...) }` block.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.kapt) apply false
}
