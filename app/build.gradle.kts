import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.android.gms.oss-licenses-plugin")
    kotlin("plugin.serialization") version "2.1.0"
}

val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("secrets/keystore.properties")
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
}

fun signingValue(name: String): String? {
    return System.getenv(name)
        ?: keystoreProperties.getProperty(name)
        ?: (project.findProperty(name) as String?)
}

android {
    namespace = "com.buenotty.blockfy"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.buenotty.blockfy"
        minSdk = 28
        targetSdk = 35
        versionCode = 35
        versionName = "1.9.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val storePath = signingValue("BLOCKFY_STORE_FILE")
    val storePasswordValue = signingValue("BLOCKFY_STORE_PASSWORD")
    val keyAliasValue = signingValue("BLOCKFY_KEY_ALIAS")
    val keyPasswordValue = signingValue("BLOCKFY_KEY_PASSWORD")
    val storeFileResolved = storePath?.let { path ->
        val file = File(path)
        if (file.isAbsolute) file else rootProject.file(path)
    }

    signingConfigs {
        if (storeFileResolved != null &&
            storeFileResolved.isFile &&
            !storePasswordValue.isNullOrBlank() &&
            !keyAliasValue.isNullOrBlank() &&
            !keyPasswordValue.isNullOrBlank()
        ) {
            create("release") {
                storeFile = storeFileResolved
                storePassword = storePasswordValue
                keyAlias = keyAliasValue
                keyPassword = keyPasswordValue
                storeType = "PKCS12"
            }
        }
    }

    buildTypes {
        debug {
            // Use the SDK debug key. Never reuse the Play upload key.
        }
        release {
            signingConfigs.findByName("release")?.let { signingConfig = it }
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
    buildFeatures {
        compose = true
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.androidx.workmanager)
    implementation(libs.protobuf.javalite)
    implementation(libs.androidx.icons.extended)
    implementation(libs.androidx.workmanager)
    implementation(libs.androidx.datastore)
    implementation(libs.ossLicenses)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.qrcode)
}
