plugins {
    id("com.android.application")
    // 1. Añadimos el plugin de Google Services
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.trabajoapi"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.trabajoapi"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    // Retrofit (API REST)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    implementation("org.osmdroid:osmdroid-android:6.1.18")
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // --- FIREBASE ---
    // Importamos el BoM (Bill of Materials) para gestionar versiones automáticamente
    implementation(platform("com.google.firebase:firebase-bom:33.9.0")) // Usamos una versión reciente y estable

    // Firebase Analytics (Recomendado por defecto)
    implementation("com.google.firebase:firebase-analytics")

    // Firebase Cloud Messaging (NECESARIO PARA NOTIFICACIONES)
    implementation("com.google.firebase:firebase-messaging")
}