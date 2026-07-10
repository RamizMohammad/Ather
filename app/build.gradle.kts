plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.secrets)
    alias(libs.plugins.hilt)
    // Mappls Services Plugin: consumes the <appId>.a.conf / <appId>.a.olf
    // configuration files in this module's root directory (replaces API keys).
    id("com.mappls.services.android")
}

android {
    namespace = "in.mohammad.ramiz.travel"
    compileSdk = 35

    defaultConfig {
        applicationId = "in.mohammad.ramiz.travel"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Mappls auth now comes from the .a.conf/.a.olf configuration files;
        // only the WeatherAPI.com key remains (set in local.properties).
        buildConfigField("String", "WEATHER_API_KEY", "\"\"")
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
        buildConfig = true
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
}

dependencies {
    // AndroidX core
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.fragment)

    // Lifecycle (Java-friendly: LiveData + ViewModel)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.androidx.lifecycle.service)

    // Room (Java annotation processor)
    implementation(libs.androidx.room.runtime)
    annotationProcessor(libs.androidx.room.compiler)

    // Hilt
    implementation(libs.hilt.android)
    annotationProcessor(libs.hilt.compiler)
    // Hilt + WorkManager
    implementation(libs.androidx.hilt.work)
    annotationProcessor(libs.androidx.hilt.compiler)

    // WorkManager
    implementation(libs.androidx.work.runtime)

    // Retrofit + Gson
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)
    implementation(libs.gson)

    // Mappls (MapmyIndia) Map SDK + REST services (map, routing, search bundled)
    implementation(platform(libs.mappls.bom))
    implementation(libs.mappls.android.sdk)

    // Google Play Services: Fused Location only (no API key needed)
    implementation(libs.play.services.location)

    // Preferences DataStore (Java via RxJava3)
    implementation(libs.datastore.preferences.rxjava3)
    implementation(libs.rxjava)

    // Glide
    implementation(libs.glide)
    annotationProcessor(libs.compiler)

    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.core.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
