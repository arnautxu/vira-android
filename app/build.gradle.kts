plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
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

    // Una sola clau de proves per a tothom (la CI inclosa): sense ella, cada APK de la CI sortia
    // signat amb una clau nova, el mòbil no deixava instal·lar-lo a sobre de l'anterior i Google no
    // podia reconèixer l'app al selector de comptes. No és secreta: només signa les proves.
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
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
    testOptions {
        // Room i les preferències de les proves corren sobre Robolectric.
        unitTests.isIncludeAndroidResources = true
        unitTests.isReturnDefaultValues = true
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

    // Dades i sincronització (`core/data`, `core/backend`)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.supabase.auth)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.okhttp)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play)
    implementation(libs.google.id)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.lifecycle.process)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(libs.androidx.compose.ui.tooling)
}
