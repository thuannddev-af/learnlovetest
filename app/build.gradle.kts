import java.text.SimpleDateFormat
import java.util.Date

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.parcelize")
    id("com.google.gms.google-services")


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
        buildConfigField("String", "adjust_token", "\"\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        multiDexEnabled = true

        val formattedDate = SimpleDateFormat("MM.dd.yyyy").format(Date())
        base.archivesName.set("ProjectBase_v${versionName}(${versionCode})_${formattedDate}")
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
        buildConfig = true
        compose = true
        viewBinding = true
    }

    flavorDimensions += "default"
    productFlavors {
        create("appDebug") {
            manifestPlaceholders["ad_app_id"] = "ca-app-pub-3940256099942544~3347511713"
            buildConfigField("String", "inter_splash", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "inter_save", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "inter_day", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "inter_love", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "inter_add", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "appopen_resume", "\"ca-app-pub-3940256099942544/9257395921\"")

            buildConfigField("Boolean", "build_debug", "true")
        }
        create("appProduct") {
            manifestPlaceholders["ad_app_id"] ="ca-app-pub-3940256099942544~3347511713"
            buildConfigField("String", "inter_splash", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "inter_save", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "inter_day", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "inter_love", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "inter_add", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "appopen_resume", "\"ca-app-pub-3940256099942544/9257395921\"")

            buildConfigField("Boolean", "build_debug", "false")
        }
    }
}

dependencies {

    implementation(project(":ads"))

    implementation("androidx.viewpager2:viewpager2:1.1.0")
    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2026.02.01"))

    // Activity
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.activity:activity-ktx:1.13.0")

    //Gson
    implementation("com.google.code.gson:gson:2.13.2")

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
    implementation("androidx.lifecycle:lifecycle-process:2.10.0")

    // Material
    implementation("com.google.android.material:material:1.10.0")

    // Unit Test
    testImplementation("junit:junit:4.13.2")

    //shimmer
    implementation("com.facebook.shimmer:shimmer:0.5.0")

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

    // Firebase BOM - quản lý phiên bản Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.4.0"))

    // Firebase Remote Config - bật/tắt quảng cáo từ xa
    implementation("com.google.firebase:firebase-config")

    // Firebase Analytics - tùy chọn nhưng hữu ích cho Remote Config
    implementation("com.google.firebase:firebase-analytics")

    // Google AdMob SDK
    implementation("com.google.android.gms:play-services-ads:24.7.0")


}
