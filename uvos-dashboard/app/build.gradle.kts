plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.uvos.dashboard"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.uvos.dashboard"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

// Намеренно ноль зависимостей: только Kotlin stdlib (добавляется автоматически).
// Меньше скачиваний - быстрее первая сборка на медленном канале.
