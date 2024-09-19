import java.io.FileInputStream
import java.util.Properties

val kotlinVersion: String by rootProject.extra

plugins {
    kotlin("android")
    kotlin("kapt")
    id("com.android.application")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("de.mannodermaus.android-junit5")
    id("com.google.devtools.ksp")
    id("org.jmailen.kotlinter")
}

fun loadProperties(fileName: String): Properties {
    val properties = Properties()
    val propertiesFile = rootProject.file("signing/$fileName")
    if (propertiesFile.exists()) {
        properties.load(FileInputStream(propertiesFile))
    }
    return properties
}

android {
    namespace = "com.neilturner.aerialviews"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.neilturner.aerialviews"
        minSdk = 22 // to support Fire OS 5, Android v5.1, Lvl 22
        targetSdk = 34
        versionCode = 24
        versionName = "1.7.3"

        manifestPlaceholders["analyticsCollectionEnabled"] = false
        manifestPlaceholders["crashlyticsCollectionEnabled"] = false
        manifestPlaceholders["performanceCollectionEnabled"] = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }

    buildFeatures {
        dataBinding = true
        buildConfig = true
    }

    // App bundle (not APK) should contain all languages so 'locale switch'
    // feature works on Play Store and Amazon Appstore builds
    // https://stackoverflow.com/a/54862243/247257
    bundle {
        language {
            enableSplit = false
        }
    }

    kotlin {
        sourceSets.configureEach {
            languageSettings.languageVersion = "2.0"
        }
    }

    buildTypes {
        getByName("debug") {
            buildConfigField("String", "BUILD_TIME", "\"${System.currentTimeMillis()}\"")

            applicationIdSuffix = ".debug"
            isDebuggable = true
            isMinifyEnabled = false

            // isPseudoLocalesEnabled = true
            // isMinifyEnabled = true
        }
        getByName("release") {
            buildConfigField("String", "BUILD_TIME", "\"${System.currentTimeMillis()}\"")

            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

            manifestPlaceholders["analyticsCollectionEnabled"] = true
            manifestPlaceholders["crashlyticsCollectionEnabled"] = true
            manifestPlaceholders["performanceCollectionEnabled"] = true

            // isDebuggable = true
        }
    }

    signingConfigs {
        create("release") {
            val releaseProps = loadProperties("release.properties")
            storeFile = releaseProps["storeFile"]?.let { file(it) }
            storePassword = releaseProps["storePassword"] as String?
            keyAlias = releaseProps["keyAlias"] as String?
            keyPassword = releaseProps["keyPassword"] as String?
        }
        create("legacy") {
            val releaseProps = loadProperties("legacy.properties")
            storeFile = releaseProps["storeFile"]?.let { file(it) }
            storePassword = releaseProps["storePassword"] as String?
            keyAlias = releaseProps["keyAlias"] as String?
            keyPassword = releaseProps["keyPassword"] as String?
        }
    }

    flavorDimensions += "version"
    productFlavors {
        create("github") {
            signingConfig = signingConfigs.getByName("legacy")
            dimension = "version"
        }
        create("beta") {
            signingConfig = signingConfigs.getByName("legacy")
            dimension = "version"
            isDefault = true
            versionNameSuffix = "-beta1"
        }
        create("googleplay") {
            signingConfig = signingConfigs.getByName("release")
            dimension = "version"
        }
        create("amazon") {
            signingConfig = signingConfigs.getByName("release")
            dimension = "version"
        }
        create("fdroid") {
            signingConfig = signingConfigs.getByName("release")
            dimension = "version"
        }
    }

    // Using this method https://stackoverflow.com/a/30548238/247257
    sourceSets {
        getByName("github").java.srcDir("src/common/java")
        getByName("beta").java.srcDir("src/common/java")
        getByName("googleplay").java.srcDir("src/common/java")
        getByName("amazon").java.srcDir("src/common/java")
        getByName("fdroid").java.srcDir("src/froid/java")
    }
}

dependencies {
    // Support all favors except F-Droid
    "githubImplementation"(libs.bundles.firebase)
    "betaImplementation"(libs.bundles.firebase)
    "googleplayImplementation"(libs.bundles.firebase)
    "amazonImplementation"(libs.bundles.firebase)

    implementation(libs.kotlin.stdlib)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    implementation(libs.core.ktx)
    implementation(libs.leanback)
    implementation(libs.leanback.preference)
    implementation(libs.preference.ktx)
    implementation(libs.activity.ktx)
    implementation(libs.constraintlayout)
    implementation(libs.appcompat)

    implementation(libs.retrofit)
    implementation(libs.converter.moshi)
    implementation(libs.logging.interceptor)
    implementation(libs.moshi.kotlin)
    ksp(libs.moshi.codegen)
    implementation(libs.gson)

    implementation(libs.media3.exoplayer)

    implementation(libs.flowbus)
    implementation(libs.flowbus.android)

    implementation(libs.kotpref)
    implementation(libs.kotpref.initializer)
    implementation(libs.kotpref.enum.support)

    implementation(libs.smbj)

    implementation(libs.sardine.android)

    implementation(libs.coil)
    implementation(libs.coil.gif)

    implementation(libs.timber)

    debugImplementation(libs.leakcanary)

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}


tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        events("started", "skipped", "passed", "failed")
        showStandardStreams = true
    }
}