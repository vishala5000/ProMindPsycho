plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.quotevideogenerator"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.quotevideogenerator"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("dev.ffmpegkit-maintained:ffmpeg-kit-full-gpl:8.1.7")
}
