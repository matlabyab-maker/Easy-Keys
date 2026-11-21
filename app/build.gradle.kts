plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    sourceSets {
        getByName("main") {
            // Kotlin/Java source files are kept in the shallow repository-level Source folder.
            java.srcDir("../Source")
            res.srcDirs("src/res")
        }
    }
    namespace = "com.easykeyboard.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.easykeyboard.app"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
