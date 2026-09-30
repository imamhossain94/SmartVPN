import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
}

android {
    namespace = "com.newagedevs.smartvpn"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.newagedevs.smartvpn"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 11
        versionName = "1.0.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // AppCompat and Material ship ~100 locale translations that this app never
    // shows. Dropping them keeps the APK/AAB meaningfully smaller.
    androidResources {
        localeFilters += "en"
    }

    // The VPN engine loads its OpenVPN/JBCrypto shared objects from the app's
    // native library directory, so the .so files must be extracted at install
    // time rather than loaded directly from an uncompressed APK page.
    packaging {
        jniLibs.useLegacyPackaging = true
        resources.excludes += setOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "/META-INF/DEPENDENCIES",
            "/META-INF/LICENSE*",
        )
    }

    // Ship a single universal .so set instead of per-ABI splits.
    bundle {
        abi { enableSplit = false }
    }

    buildFeatures {
        dataBinding = true
        buildConfig = true
    }

    // The Play Store scraping metadata leaks the applicationId for no benefit.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = true
        warningsAsErrors = false
        disable += setOf("GradleDependency", "OldTargetApi", "UnusedAttribute")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlin.time.ExperimentalTime",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
        )
    }
}

dependencies {
    implementation(project(":vpnLib"))

    constraints {
        // The Kotlin compiler can only read library metadata up to a version ahead
        // of itself. Pinning the stdlib stops a transitive dependency from
        // dragging in a newer one and breaking the whole compile.
        implementation(libs.kotlin.stdlib) {
            version { strictly(libs.versions.kotlinStdlib.get()) }
        }
    }

    // AndroidX / Material
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.localbroadcastmanager)
    implementation(libs.androidx.swiperefreshlayout)

    // Architecture components
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // DI (Koin 4 ships the viewModel DSL from koin-android itself)
    implementation(libs.koin.android)

    // Networking / serialization
    implementation(libs.okhttp)
    implementation(libs.gson)
    implementation(libs.kotlin.csv)

    // UI helpers
    implementation(libs.titlebar)
    implementation(libs.balloon)
    implementation(libs.glide)
    implementation(libs.shimmer)
    implementation(libs.recyclerview.animators)
    implementation(libs.bindables)
    implementation(libs.timber)

    // Unit tests
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.arch.core.testing)

    // Instrumented tests
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
