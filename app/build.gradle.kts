plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "net.forestany.boardgamemoney"
    compileSdk = 36

    defaultConfig {
        applicationId = "net.forestany.boardgamemoney"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    sourceSets {
        getByName("main") {
            res {
                srcDirs("src/main/res", "src/main/res/menu")
            }
        }
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.preference)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("net.forestany.forestj.net:fJ-net-lib:1.0.4")
    implementation("com.github.yukuku:ambilwarna:2.0.1")
}