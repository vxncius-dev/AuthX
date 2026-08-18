
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
}

import java.util.Properties

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use(::load)
    }
}

fun signingProperty(name: String): String? {
    return localProperties.getProperty(name) ?: System.getenv(name)
}

android {
    namespace = "com.vxncius.authx"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.vxncius.authx"
        minSdk = 24
        targetSdk = 35
        versionCode = 12200
        versionName = "1.22.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file(signingProperty("AUTHX_RELEASE_STORE_FILE") ?: "authx-release.keystore")
            storePassword = signingProperty("AUTHX_RELEASE_STORE_PASSWORD")
            keyAlias = signingProperty("AUTHX_RELEASE_KEY_ALIAS")
            keyPassword = signingProperty("AUTHX_RELEASE_KEY_PASSWORD")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }

        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            // proguardFiles(
            //     getDefaultProguardFile("proguard-android-optimize.txt"),
            //     "proguard-rules.pro"
            // )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.biometric:biometric:1.1.0")

    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.core:core-splashscreen:1.2.0")
    
    // TOTP
    implementation("dev.turingcomplete:kotlin-onetimepassword:2.1.0")
    implementation("commons-codec:commons-codec:1.15")

    // CameraX for QR scanning
    val camerax_version = "1.3.1"
    implementation("androidx.camera:camera-core:$camerax_version")
    implementation("androidx.camera:camera-camera2:$camerax_version")
    implementation("androidx.camera:camera-lifecycle:$camerax_version")
    implementation("androidx.camera:camera-view:$camerax_version")

    // ML Kit for QR scanning
    implementation("com.google.mlkit:barcode-scanning:17.2.0")

    // Autofill
    implementation("androidx.autofill:autofill:1.1.0")

    // Room
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    kapt("androidx.room:room-compiler:$room_version")
    
    // SQLCipher for database encryption
    implementation("net.zetetic:android-database-sqlcipher:4.5.3")

    // Image loading
    implementation("io.coil-kt:coil-compose:2.5.0")

    // DataStore for settings
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Google Fonts
    implementation("androidx.compose.ui:ui-text-google-fonts:1.5.4")

    // Lottie (splash screen animation)
    implementation("com.airbnb.android:lottie-compose:6.2.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
