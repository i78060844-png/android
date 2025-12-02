plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.android.swingmusic.home"
    compileSdk = 36

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
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
    implementation(project(":database"))
    implementation(project(":network"))
    implementation(project(":uicomponent"))
    implementation(project(":endlesssound"))
    implementation(project(":feature:player"))
    implementation(project(":feature:folder"))
    // Common
    implementation(project(":feature:common"))

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Coil
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Navigation
    implementation("io.github.raamcosta.compose-destinations:core:2.3.0")
    ksp("io.github.raamcosta.compose-destinations:ksp:2.3.0")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.5.0")
    
    // Hilt
    implementation("com.google.dagger:hilt-android:2.57.2")
    ksp("com.google.dagger:hilt-android-compiler:2.57.2")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
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
    arg("compose-destinations.moduleName", "feature:home")
}
