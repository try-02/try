plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room3)
    alias(libs.plugins.kotlin.serialization)
}

// Konfigurasi Room Auto-Migration Schema (sekarang di :shared)
room3 {
    schemaDirectory("$projectDir/schemas")
}

kotlin {
    // AGP 9: pakai androidMultiplatformLibrary (bukan com.android.library)
    android {
        namespace = "com.sentral.org.shared"
        compileSdk = 37
        minSdk = 26
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            // Database (Room 3 KMP)
            implementation(libs.room3.runtime)
            implementation(libs.room3.paging)
            implementation(libs.sqlite.bundled)      // cross-platform, konsisten
            // Async
            implementation(libs.coroutines.core)
            // DI (Koin core KMP)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            // Serialization
            implementation(libs.kotlinx.serialization.json)
        }
        androidMain.dependencies {
            implementation(libs.room3.runtime)
            implementation(libs.koin.android)
            implementation(libs.coroutines.android)
        }
        iosMain.dependencies {
            // NativeSQLiteDriver butuh linker -lsqlite3 (lihat下面)
            // atau tetap sqlite-bundled (cross-platform, no linker flag).
            // Default kita pakai sqlite-bundled di commonMain; baris ini opsional.
        }
        commonTest.dependencies {
            implementation(libs.junit)
            implementation(libs.kotlinx.coroutines.test)
            // (Room 3 runtime dan BundledSQLiteDriver sudah otomatis tersedia dari commonMain)
        }
    }
}

// Room KSP harus dijalankan per target source set
dependencies {
    add("kspCommonMainMetadata", libs.room3.compiler)
    add("kspAndroid", libs.room3.compiler)
    add("kspIosArm64", libs.room3.compiler)
    add("kspIosSimulatorArm64", libs.room3.compiler)
}

// Hanya berlaku kalau pakai NativeSQLiteDriver di iOS (bukan BundledSQLiteDriver)
// tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink>().configureEach {
//     kotlinOptions.freeCompilerArgs += "-linker-option" + " -lsqlite3"
// }
