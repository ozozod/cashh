plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.vayvene" // deja el namespace acá (NO en el Manifest)
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.vayvene"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // URL de tu API (con / final)
        buildConfigField("String", "BASE_URL", "\"http://192.168.1.28:3000/\"")
    }

    buildTypes {
        debug {
            isDebuggable = true
            buildConfigField("String", "BASE_URL", "\"http://192.168.1.28:3000/\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "BASE_URL", "\"http://192.168.1.28:3000/\"")
        }
    }

    buildFeatures {
        // Usás BuildConfig y (por tus logs) DataBinding/ViewBinding
        buildConfig = true
        viewBinding = true
        dataBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources.excludes += setOf(
            "META-INF/DEPENDENCIES",
            "META-INF/NOTICE.md",
            "META-INF/LICENSE.md"
        )
    }
}

dependencies {
    // Kotlin BOM para asegurar 1.9.22 en TODOS los artefactos de Kotlin
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.9.22"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib")

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("com.google.android.material:material:1.12.0")

    // Jetpack Lifecycle (para viewModelScope, etc.)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")

    // DataStore (para tus AuthStore*)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Retrofit + Gson
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    // OkHttp + logging (arregla HttpLoggingInterceptor/level)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("com.google.android.material:material:1.11.0")
}

// NOTA IMPORTANTE:
// No declares manualmente 'androidx.databinding:databinding-ktx' ni 'viewbinding' con otra versión.
// AGP ya trae lo necesario y con esto evitamos que entre 8.12.1 (que te rompía con Kotlin 2.1.x).
