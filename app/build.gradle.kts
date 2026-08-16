import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("keystore.properties")
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}

val signingStoreFile = (keystoreProperties.getProperty("storeFile") ?: System.getenv("ANDROID_KEYSTORE_PATH"))
    ?.takeIf { it.isNotBlank() }
val signingStorePassword = (keystoreProperties.getProperty("storePassword") ?: System.getenv("KEYSTORE_PASSWORD"))
    ?.takeIf { it.isNotBlank() }
val signingKeyAlias = (keystoreProperties.getProperty("keyAlias") ?: System.getenv("KEY_ALIAS"))
    ?.takeIf { it.isNotBlank() }
val signingKeyPassword = (keystoreProperties.getProperty("keyPassword") ?: System.getenv("KEY_PASSWORD"))
    ?.takeIf { it.isNotBlank() }

val hasSigningConfig = signingStoreFile != null && signingStorePassword != null &&
    signingKeyAlias != null && signingKeyPassword != null

val overrideVersionCode = providers.gradleProperty("versionCode").orNull?.toIntOrNull()
val overrideVersionName = providers.gradleProperty("versionName").orNull

android {
    namespace = "dev.wolly.dsbmaterial"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "dev.wolly.dsbmaterial"
        minSdk = 28
        targetSdk = 36
        versionCode = overrideVersionCode ?: 32
        versionName = overrideVersionName ?: "2.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasSigningConfig) {
            create("release") {
                storeFile = file(signingStoreFile!!)
                storePassword = signingStorePassword!!
                keyAlias = signingKeyAlias!!
                keyPassword = signingKeyPassword!!
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("release")
        }
        create("dev") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.findByName("release")
            versionNameSuffix = "-dev"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.google.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.gson)
    
    implementation(libs.jsoup)
    implementation(libs.okhttp)
    implementation(libs.okhttp.urlconnection)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.nanohttpd)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
