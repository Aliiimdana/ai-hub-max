plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.aihubmax.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aihubmax.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "1.0.0"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:design"))
    implementation(project(":core:network"))
    implementation(project(":core:security"))
    implementation(project(":core:routing"))
    implementation(project(":core:database"))
    implementation(project(":core:storage"))
    implementation(project(":features:home"))
    implementation(project(":features:api_manager"))
    implementation(project(":features:free_ai"))
    implementation(project(":features:chat"))
    implementation(project(":features:image_studio"))
    implementation(project(":features:video_studio"))
    implementation(project(":features:agents"))
    implementation(project(":features:documents"))
    implementation(project(":features:usage"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
