plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.overgram"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.overgram"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Hilt (DI)
    implementation("com.google.dagger:hilt-android:2.60")
    ksp("com.google.dagger:hilt-android-compiler:2.60")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Voyager (навигация)
    implementation("cafe.adriel.voyager:voyager-navigator:1.1.0-beta03")
    implementation("cafe.adriel.voyager:voyager-transitions:1.1.0-beta03")
    implementation("cafe.adriel.voyager:voyager-hilt:1.1.0-beta03")

    // Retrofit + OkHttp (REST + WebSocket)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Room (локальное хранилище / история сообщений)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Coil (загрузка изображений/аватаров)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // DataStore (настройки пользователя)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Firebase Cloud Messaging (push-уведомления)
    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
    implementation("com.google.firebase:firebase-messaging-ktx")

    // WorkManager (фоновые задачи)
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Coroutines / Flow
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Логирование / отладка сети
    implementation("com.jakewharton.timber:timber:5.0.1")
    debugImplementation("com.github.chuckerteam.chucker:library:4.0.0")
    releaseImplementation("com.github.chuckerteam.chucker:library-no-op:4.0.0")

    // Gson
    implementation("com.google.code.gson:gson:2.11.0")
}