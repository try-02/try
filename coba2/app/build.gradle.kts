plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room3)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.stability.analyzer)
}

// Konfigurasi Room Auto-Migration Schema
room3 {
    schemaDirectory("$projectDir/schemas")
}

android {
    namespace = "com.sentral.org"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.sentral.org"
        minSdk = 26 // Android 8.0 (Ideal untuk library ESCPOS & CameraX)
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    androidResources {
        localeFilters += listOf("id", "en")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // Perbaikan 3: Menggunakan Java Toolchain (Sinkron dengan target kompilasi Kotlin & Java)
    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    sourceSets {
        named("androidTest") {
            assets.directories.add(layout.projectDirectory.dir("schemas").toString())
        }
        named("debug") {
            assets.directories.add(layout.projectDirectory.dir("schemas").toString())
        }
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/*.version",
                "META-INF/INDEX.LIST"
            )

            pickFirsts += setOf(
                "META-INF/services/javax.xml.stream.XMLInputFactory",
                "META-INF/services/javax.xml.stream.XMLOutputFactory",
                "META-INF/services/javax.xml.stream.XMLEventFactory",
                "META-INF/services/org.codehaus.stax2.validation.XMLValidationSchemaFactory.DTD",
                "META-INF/services/org.codehaus.stax2.validation.XMLValidationSchemaFactory.RELAXNG",
                "META-INF/services/org.codehaus.stax2.validation.XMLValidationSchemaFactory.W3C"
            )
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

dependencies {
    // 1. AndroidX Core & Lifecycle
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.core.splash)
    implementation(libs.google.material)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // 2. Jetpack Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icon)
    implementation(libs.androidx.compose.material.icon.ext)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Navigation & Paging
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    // 4. Room 3.0.1 & SQLite Bundled 
    implementation(libs.room3.runtime)
    implementation(libs.room3.paging)
    ksp(libs.room3.compiler)
    implementation(libs.sqlite.bundled)

    // 5. Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // 6. Dependency Injection (Koin)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.android)
    implementation(libs.kotlinx.serialization.json)

    // 7. Hardware: Printer Termal
    implementation(libs.escpos.printer)

    // 8. Hardware: Pemindai Barcode (CameraX + MLKit)
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    implementation(libs.mlkit.barcode)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    lintChecks(libs.slack.compose.lints)

    // 9. Laporan Ekspor/Impor (FastExcel & Stax)
    implementation(libs.fastexcel)
    implementation(libs.aalto.xml)
    implementation(libs.stax.api)

    // ===== TESTING =====
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.room3.testing)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
