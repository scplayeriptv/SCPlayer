plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// رقم الـ Build من GitHub Actions (-PappVersionCode=run_number)؛ بيستخدمه التحديث داخل التطبيق
val buildNumber = (project.findProperty("appVersionCode") as String?)?.toIntOrNull() ?: 1

android {
    namespace = "com.sc.player"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.sc.player"
        minSdk = 24
        targetSdk = 34
        versionCode = buildNumber
        versionName = "0.1.$buildNumber"
        // libmpv ثقيلة: نكتفي بمعماريات الموبايل/التلفزيون الحقيقية
        buildConfigField("boolean", "PLAY", "false")
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    // توقيع نسخة Google Play: بيتقري من متغيرات البيئة (من GitHub Secrets) ومش موجود في الكود
    val playKs = System.getenv("PLAY_KEYSTORE_FILE")
    if (playKs != null && file(playKs).exists()) {
        signingConfigs.create("play") {
            storeFile = file(playKs)
            storePassword = System.getenv("PLAY_STORE_PASSWORD")
            keyAlias = System.getenv("PLAY_KEY_ALIAS")
            keyPassword = System.getenv("PLAY_KEY_PASSWORD")
        }
    }

    buildTypes {
        release { isMinifyEnabled = false }
        // نسخة Google Play: دفع عن طريق Play Billing، من غير تحديث ذاتي ولا إذن تثبيت APK
        create("play") {
            initWith(getByName("release"))
            isMinifyEnabled = false
            buildConfigField("boolean", "PLAY", "true")
            signingConfigs.findByName("play")?.let { signingConfig = it }
            matchingFallbacks += listOf("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation("org.apache.commons:commons-compress:1.26.2")
    implementation("com.android.billingclient:billing:7.1.1")
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.navigation:navigation-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.5")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")
    implementation("com.google.zxing:core:3.5.3")
    implementation("dev.jdtech.mpv:libmpv:0.5.1")
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
    implementation("androidx.media3:media3-cast:1.4.1")
    implementation("com.google.android.gms:play-services-cast-framework:21.5.0")
    implementation("androidx.mediarouter:mediarouter:1.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
