plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.nikita.workoutstudio"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.nikita.workoutstudio"
        minSdk = 26
        targetSdk = 34
        versionCode = 5
        versionName = "1.4"
        vectorDrawables { useSupportLibrary = true }
    }

    // Release signing driven by environment variables so the CI can inject a
    // persistent keystore from GitHub Secrets. Every build then carries the SAME
    // signature, which is what lets users install a new version over an existing
    // one (updating in place) without "package conflict" / data loss. When the
    // env vars are absent (e.g. a local build without the keystore) we skip the
    // config and Gradle falls back to unsigned/debug so the build still succeeds.
    val keystorePath = System.getenv("KEYSTORE_FILE")
    val hasReleaseKeystore = keystorePath != null && file(keystorePath).exists()
    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = file(keystorePath!!)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Use the persistent release key when available; otherwise leave the
            // default so `assembleRelease` doesn't fail locally without secrets.
            if (hasReleaseKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
}
