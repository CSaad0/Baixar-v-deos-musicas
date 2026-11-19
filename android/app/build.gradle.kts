plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val ytdlVersion = "0.18.1"

android {
    namespace = "com.baixador.musicas"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.baixador.musicas"
        minSdk = 24
        targetSdk = 35
        versionCode = (System.getenv("VERSION_CODE") ?: "1").toInt()
        versionName = "1.0." + (System.getenv("VERSION_CODE") ?: "0")
    }

    signingConfigs {
        // Chave fixa (de desenvolvimento) para que atualizações instalem por cima.
        create("fixa") {
            storeFile = file("baixador.keystore")
            storePassword = "android"
            keyAlias = "baixador"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("fixa")
        }
        debug {
            signingConfig = signingConfigs.getByName("fixa")
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = true
        }
    }

    packaging {
        // Necessário para o Python/FFmpeg embutidos serem extraídos no aparelho
        jniLibs { useLegacyPackaging = true }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("io.github.junkfood02.youtubedl-android:library:$ytdlVersion")
    implementation("io.github.junkfood02.youtubedl-android:ffmpeg:$ytdlVersion")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
}
