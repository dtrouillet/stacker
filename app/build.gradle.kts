import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

// Release signing comes from keystore.properties when building locally, and
// from environment variables in CI. With neither present the release build
// stays unsigned, so anyone can still run assembleRelease on a fresh clone.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

fun signingValue(property: String, environmentVariable: String): String? =
    (keystoreProperties.getProperty(property) ?: System.getenv(environmentVariable))
        ?.takeIf { it.isNotBlank() }

val releaseStoreFile = signingValue("storeFile", "ANDROID_KEYSTORE_FILE")
val releaseStorePassword = signingValue("storePassword", "ANDROID_KEYSTORE_PASSWORD")
val releaseKeyAlias = signingValue("keyAlias", "ANDROID_KEY_ALIAS")
val releaseKeyPassword = signingValue("keyPassword", "ANDROID_KEY_PASSWORD")
val canSignRelease = releaseStoreFile != null && releaseStorePassword != null &&
    releaseKeyAlias != null && releaseKeyPassword != null

android {
    namespace = "fr.damientrouillet.stacker"
    compileSdk = 35

    defaultConfig {
        applicationId = "fr.damientrouillet.stacker"
        minSdk = 26
        targetSdk = 35
        // Overridden by the release workflow with -PversionName / -PversionCode.
        versionCode = (project.findProperty("versionCode") as String?)?.toInt() ?: 1
        versionName = (project.findProperty("versionName") as String?) ?: "1.0.0"
    }

    signingConfigs {
        if (canSignRelease) {
            create("release") {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        debug {
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (canSignRelease) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    lint {
        // Lint has its own continuous integration step that publishes the
        // report, so it does not gate packaging. Flip abortOnError back on once
        // the report is clean.
        abortOnError = false
        checkReleaseBuilds = false
        warningsAsErrors = false
        htmlReport = true
        xmlReport = true
        sarifReport = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity)
    testImplementation(libs.junit)
}
