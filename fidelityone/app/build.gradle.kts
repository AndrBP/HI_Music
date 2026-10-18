plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    val media3 = "1.4.1"
    val room = "2.6.1"
    val composeBom = "2024.06.00"

    // Core / lifecycle
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")

    // Jetpack Compose (BOM) + Material 3
    implementation(platform("androidx.compose:compose-bom:$composeBom"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Media3 / ExoPlayer — playback, gapless, decoders (FLAC/WAV/AIFF/MP3/AAC + DSD DSF/DFF→PCM)
    implementation("androidx.media3:media3-exoplayer:$media3")
    implementation("androidx.media3:media3-session:$media3")        // MediaSessionService
    implementation("androidx.media3:media3-common:$media3")
    implementation("androidx.media3:media3-datasource:$media3")
    implementation("androidx.media3:media3-ui:$media3")             // PlayerView se necessário

    // Room — cache da biblioteca + favoritos
    implementation("androidx.room:room-runtime:$room")
    implementation("androidx.room:room-ktx:$room")
    ksp("androidx.room:room-compiler:$room")

    // DataStore — configurações (tema, layout, EQ, crossfade, replaygain, sleep timer)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Coil — capas de álbum (arquivo local + content uri)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    debugImplementation(platform("androidx.compose:compose-bom:$composeBom"))
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-tooling-preview")
}
