pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Mengambil repositori JitPack eksternal sesuai dokumentasi resmi JitPack
        maven { url = uri("https://jitpack.io") }
    }
}

// ============================================================================
// KONFIGURASI OPTIMALISASI GRADLE 9.6.1 & MIGRASI GRADLE 10
// ============================================================================

// 1. Mematikan pencarian properti implisit ke root project demi performa Isolated Projects (Gradle 10 Ready)
enableFeaturePreview("NO_IMPLICIT_LOOKUP_IN_PARENT_PROJECTS")

// 2. Mengaktifkan optimasi I/O journal tingkat lanjut untuk mempercepat pemrosesan disk di GitHub CI
enableFeaturePreview("STABLE_CONFIGURATION_CACHE")

rootProject.name = "CobaApp"
include(":app")
