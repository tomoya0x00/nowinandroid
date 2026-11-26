/*
 * Minimal reproduction of an R8 9.5.x miscompilation of a Compose restart lambda.
 * See R8_REPRO.md at the repository root.
 *
 * Deliberately does not use the nowinandroid convention plugins, Hilt or KSP, so the only
 * inputs to R8 are this module's code, Compose and its dependencies.
 */
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose)
}

android {
    namespace = "com.example.r8repro"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.r8repro"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            // The bug only shows when R8 optimizes, i.e. minified AND non-debuggable.
            isMinifyEnabled = true
            isDebuggable = false
            // Sign with the debug key so the APK installs without a release keystore.
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.activity.compose)
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
}
