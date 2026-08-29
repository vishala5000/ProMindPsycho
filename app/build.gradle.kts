plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {

    namespace = "com.quotegenerator"

    compileSdk = 35

    defaultConfig {

        applicationId = "com.quotegenerator"

        minSdk = 24

        targetSdk = 35

        versionCode = 1

        versionName = "1.0"
    }

    /*
     * Current FFmpegKit 8.1.7 Android package
     * provides arm64-v8a.
     */
    ndk {
        abiFilters += "arm64-v8a"
    }

    compileOptions {

        sourceCompatibility =
            JavaVersion.VERSION_17

        targetCompatibility =
            JavaVersion.VERSION_17
    }

    kotlinOptions {

        jvmTarget = "17"
    }

    packaging {

        resources {

            excludes +=
                "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {

        debug {

            isMinifyEnabled = false
        }

        release {

            isMinifyEnabled = false
        }
    }
}

dependencies {

    implementation(
        "androidx.core:core-ktx:1.15.0"
    )

    implementation(
        "androidx.appcompat:appcompat:1.7.0"
    )

    implementation(
        "androidx.activity:activity-ktx:1.10.1"
    )

    implementation(
        "com.google.android.material:material:1.12.0"
    )

    /*
     * Maintained FFmpegKit.
     *
     * Includes:
     * - FFmpeg
     * - H.264/x264
     * - AAC
     * - MP3 decoding
     * - MP4 muxing
     */
    implementation(
        "dev.ffmpegkit-maintained:ffmpeg-kit-full-gpl:8.1.7"
    )
}
