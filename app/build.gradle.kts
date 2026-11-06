plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.parcelize")


}

android {
    namespace = "com.example.afica.lovetest"

    compileSdk = 36


    defaultConfig {
        applicationId = "com.example.afica.lovetest"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2026.02.01"))

    // Activity
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.activity:activity-ktx:1.13.0")

    // AppCompat
    implementation("androidx.appcompat:appcompat:1.6.1")

    // Compose
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // ConstraintLayout
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Core KTX
    implementation("androidx.core:core-ktx:1.17.0")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")

    // Material
    implementation("com.google.android.material:material:1.10.0")

    // Unit Test
    testImplementation("junit:junit:4.13.2")

    // Android Test
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.02.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")

    // Debug
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("com.google.dagger:hilt-android:2.60.1")
    ksp("com.google.dagger:hilt-compiler:2.60.1")
    implementation("androidx.room:room-runtime:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")
    implementation("androidx.fragment:fragment-ktx:1.8.9")

}
