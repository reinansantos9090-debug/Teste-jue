plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.testejuerpg"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aistudio.testejuerpg.wkvtpa"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "2.0.0-3d"
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation("junit:junit:4.13.2")
}
