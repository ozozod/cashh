plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    // ⬅️ Debe coincidir con el package base de tus clases (com/example/vayvene/…)
    namespace = "com.example.vayvene"

    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.vayvene"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // Fallback si corrés sin flavors
        buildConfigField("String", "BASE_URL", "\"http://192.168.1.28:3000\"")
    }

    // Habilita BuildConfig (necesario para los buildConfigField)
    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug { isMinifyEnabled = false }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // Flavors para separar dev/prod
    flavorDimensions += "env"
    productFlavors {
        create("dev") {
            dimension = "env"
            buildConfigField("String", "BASE_URL", "\"http://192.168.1.28:3000\"")
        }
        create("prod") {
            dimension = "env"
            buildConfigField("String", "BASE_URL", "\"https://tu-dominio.com\"")
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

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")

    // Retrofit + Moshi
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")

    // OkHttp + logging
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
}
