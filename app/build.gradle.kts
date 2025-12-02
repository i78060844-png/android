plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.android.swingmusic"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.android.swingmusic"
        minSdk = 26
        targetSdk = 36
        versionCode = project.findProperty("versionCode")?.toString()?.toInt() ?: 1
        versionName = project.findProperty("versionName")?.toString() ?: "1.0.1-night"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_FILE") ?: "release-key.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Local Modules
    implementation(project(":auth"))
    implementation(project(":core"))
    implementation(project(":network"))
    implementation(project(":uicomponent"))
    implementation(project(":endlesssound"))
    // Feature Modules
    implementation(project(":feature:home"))
    implementation(project(":feature:folder"))
    implementation(project(":feature:player"))
    implementation(project(":feature:artist"))
    implementation(project(":feature:album"))
    implementation(project(":feature:search"))
    // Common Feature
    implementation(project(":feature:common"))

    // Core
   // implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2025.11.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // Hilt DI
    implementation("com.google.dagger:hilt-android:2.57.2")
    ksp("com.google.dagger:hilt-android-compiler:2.57.2")

    // Hilt Navigation-Compose
    implementation("androidx.hilt:hilt-navigation-compose:1.3.0")

    // Room
    implementation("androidx.room:room-runtime:2.8.4")
    annotationProcessor("androidx.room:room-compiler:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:3.0.0")

    // Timber
    implementation("com.jakewharton.timber:timber:5.0.1")

    // WorkManger
    implementation("androidx.work:work-runtime-ktx:2.11.0")
    implementation("androidx.hilt:hilt-work:1.3.0")

    // Navigation
    // implementation("io.github.raamcosta.compose-destinations:core:1.9.63")
    implementation("io.github.raamcosta.compose-destinations:core:2.3.0")
    ksp("io.github.raamcosta.compose-destinations:ksp:2.3.0")

    // Media3-Player
    implementation("androidx.media3:media3-exoplayer:1.8.0")
    implementation("androidx.media3:media3-session:1.8.0")
    // implementation("androidx.media3:media3-exoplayer-hls:1.3.1")
    // implementation("androidx.media3:media3-exoplayer-dash:1.3.1")

    // Coil Image Loader
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
}

kotlin {
    sourceSets {
        debug {
            kotlin.srcDir("build/generated/ksp/debug/kotlin")
        }
        release {
            kotlin.srcDir("build/generated/ksp/release/kotlin")
        }
    }
}

ksp {
    arg("compose-destinations.generateNavGraphs", "false")
    arg("compose-destinations.moduleName", "app")
}

tasks.register("printVersionName") {
    doLast {
        println(android.defaultConfig.versionName)
    }
}

tasks.register("printVersionCode") {
    doLast {
        println(android.defaultConfig.versionCode)
    }
}
