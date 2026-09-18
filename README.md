# 📋 Ringkasan Proyek POS Kasir

## 🎯 Overview

| Item | Detail |
|------|--------|
| **Nama Proyek** | CobaApp (`com.sentral.org`) |
| **Jenis** | Aplikasi Point of Sale (POS) Kasir Offline |
| **Platform** | Android (utama) + iOS (via KMP) |
| **Arsitektur** | Clean Architecture + KMP (Kotlin Multiplatform) |
| **Status** | ✅ Tahap 3 Selesai — Auto-print berhasil |

---

## 🏗️ Stack Teknologi

### Core
| Layer | Teknologi | Versi |
|-------|-----------|-------|
| **Language** | Kotlin | 2.4.20-RC |
| **Build System** | Gradle | 9.x |
| **Android Gradle Plugin** | AGP | 9.5.0-alpha02 |
| **KMP Plugin** | AKML | 9.3.0-alpha03 |
| **Java Toolchain** | JDK | 21 |
| **Target SDK** | Android | 37 |
| **Min SDK** | Android | 26 (Android 8.0) |

### UI
| Item | Teknologi | Versi |
|------|-----------|-------|
| **UI Framework** | Jetpack Compose | BOM 2026.08.00 |
| **Design System** | Material 3 Expressive | 1.5.0-alpha26 |
| **Navigation** | Navigation Compose | 2.10.0-rc01 |
| **Icons** | Material Icons Extended | 1.7.8 |
| **Splash Screen** | Core SplashScreen | 1.2.0 |
| **Stability Analyzer** | Compose Stability Analyzer | 0.13.0 |
| **Lint** | Slack Compose Lints | 1.5.4 |

### Database & Persistence
| Item | Teknologi | Versi |
|------|-----------|-------|
| **Database** | Room 3 (KMP) | 3.0.2 |
| **SQLite Driver** | Bundled SQLite | 2.7.0 |
| **Paging** | Paging 3 | 3.5.1 |
| **Migration** | Auto-migration + Manual | Ready |

### Dependency Injection
| Item | Teknologi | Versi |
|------|-----------|-------|
| **DI Framework** | Koin | 4.2.2 |
| **Koin Android** | Koin Android + Compose | 4.2.2 |
| **Koin Core (KMP)** | Koin Core | 4.2.2 |

### Async & Concurrency
| Item | Teknologi | Versi |
|------|-----------|-------|
| **Coroutines** | Kotlinx Coroutines | 1.11.0 |
| **Flow** | Kotlin Flow (built-in) | - |

### Serialization
| Item | Teknologi | Versi |
|------|-----------|-------|
| **Kotlinx Serialization** | JSON | 1.11.0 |

---

## 🔌 Hardware Integration

| Item | Library | Versi | Status |
|------|---------|-------|--------|
| **Printer Thermal** | DantSu ESC/POS | 3.4.0 | ✅ Working |
| **Barcode Scanner** | CameraX + MLKit | 1.7.0-alpha03 / 17.3.0 | 🔜 Planned |

### Printer Features (Implemented)
- ✅ Bluetooth connection
- ✅ WiFi (TCP) connection
- ⚠️ USB connection (infrastruktur siap, butuh permission flow)
- ✅ Auto-print setelah checkout
- ✅ Print struk dengan QR code
- ✅ Print logo toko (dengan anti-OOM)
- ✅ Health tracking (auto-disable setelah 3x gagal)
- ✅ Queue system untuk multi-print

---

## 📦 Library Pendukung

| Kategori | Library | Versi | Fungsi |
|----------|---------|-------|--------|
| **Excel Export** | FastExcel | 0.20.2 | Laporan penjualan |
| **XML Parsing** | Aalto XML | 1.4.0 | Parse data XML |
| **StAX API** | StAX API | 1.0-2 | Streaming XML |

---

## 🧪 Testing

| Jenis | Framework | Status |
|-------|-----------|--------|
| **Unit Test** | JUnit 4 + MockK | ✅ Pass |
| **Integration Test** | Room Testing + AndroidJUnit4 | ✅ Pass |
| **Android Host Test** | JUnit (JVM) | ✅ Pass |
| **iOS Simulator Test** | XCTest (via KMP) | ✅ Pass |
| **Lint** | Android Lint + Compose Lints | ✅ Pass |

### Test Coverage
- ✅ MoneyMath (pembulatan, overflow, alokasi proporsional)
- ✅ CheckoutService (atomic transaction, rollback, race condition)
- ✅ ReturService (anti-over-refund, budget clamping)
- ✅ VoidService (symmetric reversal, double-void prevention)
- ✅ RoomTransactionRunner (commit/rollback)
- ✅ NomorTransaksiGenerator (format validation)
- ✅ PesanError (user-friendly error mapping)
- ✅ SuspendRunCatching (CancellationException propagation)

---

## 📊 Status Proyek per Tahap

### ✅ Tahap 1: Fondasi Data (SELESAI)
- [x] Database schema (15 entities)
- [x] Room 3 KMP setup
- [x] Migration infrastructure
- [x] Auto-migration ready
- [x] DatabaseWarmup + ProductSeeder
- [x] DevSessionBootstrap (temporary)

### ✅ Tahap 2: Hardware Integration (SELESAI)
- [x] PrinterService dengan queue system
- [x] EscPosPrinterDriver (Bluetooth + WiFi)
- [x] ReceiptFormatter
- [x] Logo printing (anti-OOM, auto-resize)
- [x] Printer health tracking
- [x] Add Printer UI (Bluetooth scan + WiFi form)

### ✅ Tahap 3: Auto-Print Integration (SELESAI)
- [x] Checkout → auto-print flow
- [x] Printer status indicator di TopAppBar
- [x] Printer Settings screen
- [x] Test connection dialog
- [x] Error handling + retry logic
- [x] Print optimization (QR code kecil, timeout)

### 🔜 Tahap 4: Riwayat Transaksi (BELUM DIMULAI)
- [ ] Riwayat Transaksi screen (Paging 3)
- [ ] Detail transaksi
- [ ] Cetak ulang struk
- [ ] Filter & search transaksi
- [ ] Export ke Excel/PDF

### 🔜 Tahap 5: Shift Management (BELUM DIMULAI)
- [ ] Login kasir (PIN)
- [ ] Buka shift
- [ ] Tutup shift dengan kas hitung
- [ ] Laporan shift

### 🔜 Tahap 6: Barcode Scanner (BELUM DIMULAI)
- [ ] CameraX integration
- [ ] MLKit barcode detection
- [ ] Scan → tambah produk ke keranjang

### 🔜 Tahap 7: Production Readiness (BELUM DIMULAI)
- [ ] Downgrade ke versi stable (AGP, Kotlin, Room)
- [ ] Backup & restore database
- [ ] Sync ke server (opsional)
- [ ] CI/CD pipeline
- [ ] Release signing

---

## 🏢 Modul Struktur

```
project/
├── app/                          ← Android app module
│   ├── ui/                       ← Compose UI + ViewModels
│   ├── hardware/                 ← EscPosPrinterDriver
│   ├── di/                       ← Koin AppModule
│   └── data/                     ← Repository impl (Android)
│
├── shared/                       ← KMP shared module
│   ├── commonMain/               ← Kode cross-platform
│   │   ├── data/
│   │   │   ├── entity/           ← Room entities
│   │   │   ├── dao/              ← Room DAOs
│   │   │   ├── repository/       ← Repository interfaces
│   │   │   ├── service/          ← Domain services
│   │   │   ├── model/            ← Domain models + MoneyMath
│   │   │   ├── converter/        ← Room type converters
│   │   │   └── session/          ← Session management
│   │   └── migrations/           ← Database migrations
│   ├── androidMain/              ← Android-specific
│   └── iosMain/                  ← iOS-specific
│
├── gradle/
│   └── libs.versions.toml        ← Version catalog
│
└── settings.gradle.kts           ← Module declarations
```

---

## 🏢 Struktur Lengkap Projek

```
project/
├── app
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src
│       ├── main
│       │   ├── AndroidManifest.xml
│       │   ├── assets
│       │   ├── java
│       │   │   └── com
│       │   │       └── sentral
│       │   │           └── org
│       │   │               ├── MainActivity.kt
│       │   │               ├── PosApplication.kt
│       │   │               ├── di
│       │   │               │   └── AppModule.kt
│       │   │               ├── hardware
│       │   │               │   └── EcsPosPrinterDriver.kt
│       │   │               └── ui
│       │   │                   ├── MainViewModel.kt
│       │   │                   ├── navigation
│       │   │                   │   ├── PosNavHost.kt
│       │   │                   │   └── PosRoute.kt
│       │   │                   ├── screen
│       │   │                   │   ├── pos
│       │   │                   │   │   ├── CheckoutViewModel.kt
│       │   │                   │   │   ├── KasirContract.kt
│       │   │                   │   │   ├── KasirViewModel.kt
│       │   │                   │   │   ├── PesanError.kt
│       │   │                   │   │   ├── PosDialog.kt
│       │   │                   │   │   ├── PosUtamaScreen.kt
│       │   │                   │   │   └── UangText.kt
│       │   │                   │   └── settings
│       │   │                   │       ├── AddPrinterScreen.kt
│       │   │                   │       └── PrinterSettingsScreen.kt
│       │   │                   ├── theme
│       │   │                   │   └── PosTheme.kt
│       │   │                   └── viewmodel
│       │   │                       └── AddPrinterViewModel.kt
│       │   └── res
│       │       ├── drawable
│       │       │   ├── ic_launcher_background.xml
│       │       │   └── ic_logo_pos.xml
│       │       ├── mipmap-anydpi
│       │       │   ├── ic_launcher.xml
│       │       │   └── ic_launcher_round.xml
│       │       ├── values
│       │       │   └── themes.xml
│       │       └── xml
│       │           └── file_paths.xml
│       └── test
│           └── java
│               └── com
│                   └── sentral
│                       └── org
│                           └── ui
│                               └── screen
│                                   └── pos
│                                       ├── NomorTransaksiGeneratorTest.kt
│                                       └── PesanErrorTest.kt
├── build.gradle.kts
├── gradle
│   ├── libs.versions.toml
│   └── wrapper
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── gradle.properties
├── gradlew
├── settings.gradle.kts
└── shared
    ├── build.gradle.kts
    ├── schemas
    │   └── com.sentral.org.data.PosDatabase
    │       └── 1.json
    └── src
        ├── androidMain
        │   └── kotlin
        │       └── com
        │           └── sentral
        │               └── org
        │                   ├── data
        │                   └── shared
        │                       └── Platform.android.kt
        ├── androidUnitTest
        │   └── kotlin
        │       └── com
        │           └── sentral
        │               └── org
        │                   └── data
        │                       └── migrations
        │                           └── MigrasiTest.kt
        ├── commonMain
        │   └── kotlin
        │       └── com
        │           └── sentral
        │               └── org
        │                   ├── data
        │                   │   ├── DatabaseWarmup.kt
        │                   │   ├── PosDatabase.kt
        │                   │   ├── PosDatabaseFactory.kt
        │                   │   ├── converter
        │                   │   │   └── DatabaseConverters.kt
        │                   │   ├── dao
        │                   │   │   ├── ItemKeranjangDao.kt
        │                   │   │   ├── ItemTransaksiDao.kt
        │                   │   │   ├── KasirDao.kt
        │                   │   │   ├── KeranjangDao.kt
        │                   │   │   ├── PembayaranDao.kt
        │                   │   │   ├── PergerakanKasDao.kt
        │                   │   │   ├── PergerakanPersediaanDao.kt
        │                   │   │   ├── PersediaanDao.kt
        │                   │   │   ├── PrinterDao.kt
        │                   │   │   ├── ProdukDao.kt
        │                   │   │   ├── ProfilTokoDao.kt
        │                   │   │   ├── ReturDao.kt
        │                   │   │   ├── ShiftDao.kt
        │                   │   │   └── TransaksiDao.kt
        │                   │   ├── entity
        │                   │   │   ├── ItemKeranjangEntity.kt
        │                   │   │   ├── ItemPengembalianEntity.kt
        │                   │   │   ├── ItemTransaksiEntity.kt
        │                   │   │   ├── KasirEntity.kt
        │                   │   │   ├── KeranjangEntity.kt
        │                   │   │   ├── PembayaranEntity.kt
        │                   │   │   ├── PengembalianEntity.kt
        │                   │   │   ├── PergerakanKasEntity.kt
        │                   │   │   ├── PergerakanPersediaanEntity.kt
        │                   │   │   ├── PersediaanEntity.kt
        │                   │   │   ├── PrinterEntity.kt
        │                   │   │   ├── ProdukEntity.kt
        │                   │   │   ├── ProfilTokoEntity.kt
        │                   │   │   ├── ShiftEntity.kt
        │                   │   │   ├── TransaksiDenganDetail.kt
        │                   │   │   └── TransaksiEntity.kt
        │                   │   ├── migrasi
        │                   │   │   └── migrasi.kt
        │                   │   ├── model
        │                   │   │   ├── Calculations.kt
        │                   │   │   ├── DomainException.kt
        │                   │   │   ├── DomainModels.kt
        │                   │   │   ├── Enums.kt
        │                   │   │   └── PrinterModels.kt
        │                   │   ├── repository
        │                   │   │   ├── CartRepository.kt
        │                   │   │   ├── KasirRepository.kt
        │                   │   │   ├── PersediaanRepository.kt
        │                   │   │   ├── PrinterRepository.kt
        │                   │   │   ├── ProdukRepository.kt
        │                   │   │   ├── ProfilTokoRepository.kt
        │                   │   │   ├── ReturRepository.kt
        │                   │   │   ├── ShiftRepository.kt
        │                   │   │   ├── TransaksiRepository.kt
        │                   │   │   └── impl
        │                   │   │       ├── OfflineCartRepository.kt
        │                   │   │       ├── OfflineKasirRepository.kt
        │                   │   │       ├── OfflinePersediaanRepository.kt
        │                   │   │       ├── OfflinePrinterRepository.kt
        │                   │   │       ├── OfflineProdukRepository.kt
        │                   │   │       ├── OfflineProfilTokoRepository.kt
        │                   │   │       ├── OfflineReturRepository.kt
        │                   │   │       ├── OfflineShiftRepository.kt
        │                   │   │       └── OfflineTransaksiRepository.kt
        │                   │   ├── seed
        │                   │   │   ├── ProductSeeder.kt
        │                   │   │   └── SeedProduct.kt
        │                   │   ├── service
        │                   │   │   ├── CartService.kt
        │                   │   │   ├── CheckoutService.kt
        │                   │   │   ├── InventoryMutationService.kt
        │                   │   │   ├── PersediaanService.kt
        │                   │   │   ├── PosWriteService.kt
        │                   │   │   ├── PrinterDriver.kt
        │                   │   │   ├── PrinterService.kt
        │                   │   │   ├── ReceiptFormatter.kt
        │                   │   │   ├── ReturService.kt
        │                   │   │   ├── ShiftService.kt
        │                   │   │   └── VoidService.kt
        │                   │   └── session
        │                   │       ├── DevSessionBootstrap.kt
        │                   │       └── SesiKasirProvider.kt
        │                   └── shared
        │                       └── Platform.kt
        ├── commonTest
        │   └── kotlin
        │       └── com
        │           └── sentral
        │               └── org
        │                   └── data
        │                       ├── TestDatabaseFactory.kt
        │                       ├── model
        │                       │   ├── MoneyMathTest.kt
        │                       │   └── SuspendRunCatchingTest.kt
        │                       └── service
        │                           ├── CheckoutServiceIntegrationTest.kt
        │                           ├── ReturServiceIntegrationTest.kt
        │                           ├── RoomTransactionRunnerTest.kt
        │                           └── VoidServiceIntegrationTest.kt
        └── iosMain
            └── kotlin
                └── com
                    └── sentral
                        └── org
                            ├── data
                            └── shared
                                └── Platform.ios.kt
```

---

## 🔑 Fitur Utama yang Sudah Jalan

| Fitur | Status | Keterangan |
|-------|--------|------------|
| **Katalog Produk** | ✅ Working | Grid + search + filter kategori |
| **Keranjang Multi-Session** | ✅ Working | Tahan, lanjutkan, batalkan |
| **Checkout Tunai** | ✅ Working | Dengan kembalian otomatis |
| **Checkout QRIS** | ✅ Working | Placeholder untuk integrasi payment gateway |
| **Diskon** | ✅ Working | Nominal + persentase, alokasi proporsional |
| **Retur Barang** | ✅ Working | Normal, rusak, dispose |
| **Void Transaksi** | ✅ Working | Symmetric reversal |
| **Manajemen Stok** | ✅ Working | Normal + rusak, ledger lengkap |
| **Shift Kasir** | ✅ Working | Buka/tutup, kas awal |
| **Printer Thermal** | ✅ Working | Auto-print, health tracking |
| **Laporan** | 🔜 Planned | Screen placeholder |
| **Pengaturan** | 🔜 Planned | Screen placeholder |

---

## 📈 Metrik Kode

| Metrik | Nilai |
|--------|-------|
| **Total Files** | ~80+ |
| **Lines of Code** | ~8,000+ |
| **Entities** | 15 |
| **DAOs** | 14 |
| **Services** | 8 |
| **Repositories** | 9 |
| **ViewModels** | 4 |
| **Screens** | 5 |
| **Test Cases** | 40+ |

---
