import java.util.Properties

// Task ghi private key vào assets (tránh BuildConfig string escaping issues)
val writeFcmKey by tasks.registering {
    val localPropsFile = rootProject.file("local.properties")
    val assetsDir = file("src/main/assets")
    val outputFile = assetsDir.resolve("fcm_sa_key.txt")

    inputs.file(localPropsFile)
    outputs.file(outputFile)

    doLast {
        val props = Properties()
        if (localPropsFile.exists()) {
            props.load(localPropsFile.inputStream())
        }
        val rawKey = props.getProperty("FCM_SA_PRIVATE_KEY", "")
        val cleanKey = rawKey
            .replace("\\n", "")
            .replace("\n", "")
            .replace("\r", "")
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace(" ", "")
            .replace("\t", "")

        assetsDir.mkdirs()
        outputFile.writeText(cleanKey)
    }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.nexus"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.nexus"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Đọc Service Account credentials từ local.properties (không commit lên git)
        val localProps = Properties()
        val localPropsFile = rootProject.file("local.properties")
        if (localPropsFile.exists()) {
            localProps.load(localPropsFile.inputStream())
        }
        buildConfigField("String", "FCM_SA_PROJECT_ID", "\"${localProps.getProperty("FCM_SA_PROJECT_ID", "")}\"")
        buildConfigField("String", "FCM_SA_CLIENT_EMAIL", "\"${localProps.getProperty("FCM_SA_CLIENT_EMAIL", "")}\"")
        buildConfigField("String", "FCM_SA_PRIVATE_KEY_ID", "\"${localProps.getProperty("FCM_SA_PRIVATE_KEY_ID", "")}\"")
        buildConfigField("String", "MIMO_API_KEY", "\"${localProps.getProperty("MIMO_API_KEY", "")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.animation)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Firebase
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.database)

    // WebRTC
    implementation("io.github.webrtc-sdk:android:144.7559.05")

    // Google Sign-In / Credentials
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // Image Loading
    implementation(libs.coil.compose)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Video Playback (ExoPlayer / Media3)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)


    // DataStore
    implementation(libs.datastore.preferences)

    // Biometric
    implementation(libs.androidx.biometric)

    // Security Crypto (EncryptedSharedPreferences)
    implementation(libs.androidx.security.crypto)

    // Coroutines Tasks
    implementation(libs.kotlinx.coroutines.play.services)

    // OkHttp (dùng cho FCM direct send)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Gson (JSON parsing for MiMo API)
    implementation("com.google.code.gson:gson:2.11.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

// Chạy writeFcmKey trước khi merge assets
tasks.configureEach {
    if (name.startsWith("merge") && name.contains("Assets")) {
        dependsOn(writeFcmKey)
    }
}