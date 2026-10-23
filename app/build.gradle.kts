import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Release signing. <root>/keystore.properties is gitignored and, when present, must contain:
//   storeFile=/absolute/path/to/lagoon-release.jks   (or a path relative to the repo root)
//   storePassword=...
//   keyAlias=lagoon
//   keyPassword=...
// Without the file the release build type stays UNSIGNED, so `assembleFossRelease` and
// `bundlePlayRelease` still succeed for contributors and pull-request CI; the output just
// cannot be installed. The release workflow writes this file from GitHub secrets.
// See docs/release.md for creating the keystore.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.isFile) keystorePropertiesFile.inputStream().use { load(it) }
}
val hasReleaseKeystore = !keystoreProperties.getProperty("storeFile").isNullOrBlank()

android {
    namespace = "com.lagoon.island"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.lagoon.island"
        minSdk = 31
        targetSdk = 36
        // versionCode is bumped by hand, by exactly 1, on every tagged release (Play and
        // F-Droid both reject a reused or decreasing code; F-Droid also keys its changelog on
        // it: fastlane/metadata/android/en-US/changelogs/<versionCode>.txt). It is not derived
        // from git on purpose: F-Droid builds from a clean checkout and a derived value would
        // not be reproducible. versionName must equal the release tag without the "v"
        // (tag v0.1.0 -> "0.1.0"); the release workflow fails when they differ.
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // "play": Google Play build (no accessibility service until the Play declaration is approved).
    // "foss": GitHub / F-Droid build with the optional Enhanced-mode accessibility service.
    flavorDimensions += "distribution"
    productFlavors {
        create("play") {
            dimension = "distribution"
            buildConfigField("boolean", "ENHANCED_MODE_AVAILABLE", "true")
        }
        create("foss") {
            dimension = "distribution"
            buildConfigField("boolean", "ENHANCED_MODE_AVAILABLE", "true")
        }
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
                // minSdk 31: v1 (JAR) signing is never consulted, so leave it off.
                enableV1Signing = false
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        release {
            // Sanity: a release must never ship debuggable (Play rejects it, and it would let
            // anyone attach a debugger to the notification listener).
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Signed only when keystore.properties exists; otherwise unsigned (see top of file).
            signingConfig = if (hasReleaseKeystore) signingConfigs.getByName("release") else null
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Lagoon ships en (default), ru, de, fr, es. AGP writes the locale list for the system
    // per-app language picker (Android 13+) from the values-* folders.
    androidResources {
        generateLocaleConfig = true
        // Ship only the languages Lagoon is translated into (drops libraries' extra locales).
        localeFilters += listOf("en", "ru", "de", "fr", "es")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    dependenciesInfo {
        // F-Droid: do not embed the Google Play dependency metadata blob.
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.savedstate)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.palette)
    implementation(libs.coil.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
