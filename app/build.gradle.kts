plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}



android {
    namespace = "com.example.whatsappclone"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.whatsappclone"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
}





dependencies {
    // AndroidX Core & UI
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("androidx.recyclerview:recyclerview:1.3.1")

    // Material Design
    implementation(libs.material)

    // Smack XMPP (Chat Features)
    implementation("org.igniterealtime.smack:smack-android:4.4.6") {
        exclude(group = "xpp3")
    }
    implementation("org.igniterealtime.smack:smack-tcp:4.4.6") {
        exclude(group = "xpp3")
    }
    implementation("org.igniterealtime.smack:smack-im:4.4.6") {
        exclude(group = "xpp3")
    }
    implementation("org.igniterealtime.smack:smack-extensions:4.4.6") {
        exclude(group = "xpp3")
    }
    // Glide (Image Loading)
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Lifecycle components
    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
