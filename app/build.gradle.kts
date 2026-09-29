plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.inddev.daemon"
    compileSdk = 34

    val ciVersionCode = System.getenv("BUILD_NUMBER")?.toIntOrNull() ?: 1

    defaultConfig {
        applicationId = "com.inddev.daemon"
        minSdk = 26
        targetSdk = 28
        versionCode = ciVersionCode
        versionName = "1.0.$ciVersionCode"

        ndk {
            abiFilters.clear()
            abiFilters.add("arm64-v8a")
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
        disable += "ExpiredTargetSdkVersion"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
}
