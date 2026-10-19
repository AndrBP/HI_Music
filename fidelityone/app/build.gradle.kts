plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.fidelityone.player"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.fidelityone.player"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0-mvp"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        release {
            isMinifyEnabled = false // MVP: sem proguard agressivo
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    // Bytecode Java/Kotlin em 17: binários funcionam quando o Gradle roda com JDK 17 ou JDK 21.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    // Core / lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    // Jetpack Compose (BOM) + Material 3
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.animation)
    implementation(libs.navigation.compose)

    // Media3 / ExoPlayer — playback, gapless, decoders (FLAC/WAV/AIFF/MP3/AAC + DSD DSF/DFF→PCM)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)        // MediaSessionService
    implementation(libs.media3.common)
    implementation(libs.media3.datasource)
    implementation(libs.media3.ui)             // PlayerView se necessário

    // Room — cache da biblioteca + favoritos
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // DataStore — configurações (tema, layout, EQ, crossfade, replaygain, sleep timer)
    implementation(libs.datastore.preferences)

    // Coil — capas de álbum (arquivo local + content uri)
    implementation(libs.coil.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    debugImplementation(platform(libs.compose.bom))
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.tooling.preview)
}
