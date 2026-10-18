plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.screenshot)
}

android {
    namespace = "cat.vira.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "cat.vira.android"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"
        // Els tres idiomes de l'iPhone, i cap més: la CI comprova que no divergeixin.
        resourceConfigurations += listOf("ca", "es", "en")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    // Captures de la galeria del sistema de disseny i de cada pantalla (clar, fosc i lletra gran).
    experimentalProperties["android.experimental.enableScreenshotTest"] = true
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.media3.exoplayer)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(libs.androidx.compose.ui.tooling)
}
