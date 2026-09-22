package com.sentral.org.di

import android.app.Application
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.sentral.org.data.DatabaseWarmup
import com.sentral.org.data.PosDatabase
import com.sentral.org.data.createPosDatabase
import com.sentral.org.data.concurrency.PosExecutionLock
import com.sentral.org.data.concurrency.PosExecutionLockImpl
import com.sentral.org.data.repository.impl.OfflineProdukRepository
import com.sentral.org.data.repository.impl.OfflinePersediaanRepository
import com.sentral.org.data.repository.impl.OfflineKasirRepository
import com.sentral.org.data.repository.impl.OfflineShiftRepository
import com.sentral.org.data.repository.impl.OfflineCartRepository
import com.sentral.org.data.repository.impl.OfflineTransaksiRepository
import com.sentral.org.data.repository.impl.OfflineReturRepository
import com.sentral.org.data.repository.impl.OfflinePrinterRepository
import com.sentral.org.data.repository.impl.OfflineProfilTokoRepository
import com.sentral.org.data.repository.impl.OfflineLaporanRepository
import com.sentral.org.data.repository.PersediaanRepository
import com.sentral.org.data.repository.ProdukRepository
import com.sentral.org.data.repository.KasirRepository
import com.sentral.org.data.repository.CartRepository
import com.sentral.org.data.repository.ShiftRepository
import com.sentral.org.data.repository.TransaksiRepository
import com.sentral.org.data.repository.ReturRepository
import com.sentral.org.data.repository.PrinterRepository
import com.sentral.org.data.repository.ProfilTokoRepository
import com.sentral.org.data.repository.LaporanRepository
import com.sentral.org.data.security.PinHasher
import com.sentral.org.data.security.PinRateLimiter
import com.sentral.org.data.security.createPinHasher
import com.sentral.org.data.seed.ProductSeeder
import com.sentral.org.data.service.AuthService
import com.sentral.org.data.service.PosWriteService
import com.sentral.org.data.service.InventoryMutationService
import com.sentral.org.data.service.PersediaanService
import com.sentral.org.data.service.CartService
import com.sentral.org.data.service.ShiftService
import com.sentral.org.data.service.CheckoutService
import com.sentral.org.data.service.ReturService
import com.sentral.org.data.service.VoidService
import com.sentral.org.data.service.PrinterService
import com.sentral.org.data.session.ActiveSesiKasirProvider
import com.sentral.org.data.session.DevSessionBootstrap
import com.sentral.org.data.session.SesiKasirProvider
import com.sentral.org.domain.service.ProductManagementService
import com.sentral.org.export.ExcelReportExporter
import com.sentral.org.export.TransaksiExportUseCase
import com.sentral.org.hardware.EscPosPrinterDriver
import com.sentral.org.ui.MainViewModel
import com.sentral.org.ui.screen.auth.LoginKasirViewModel
import com.sentral.org.ui.screen.pos.KasirViewModel
import com.sentral.org.ui.screen.riwayat.RiwayatViewModel
import com.sentral.org.ui.screen.settings.PrinterSettingsViewModel
import com.sentral.org.ui.screen.shift.BukaShiftViewModel
import com.sentral.org.ui.screen.shift.TutupShiftViewModel
import com.sentral.org.ui.screen.inventory.FormProdukViewModel
import com.sentral.org.ui.screen.inventory.KelolaProdukViewModel
import com.sentral.org.ui.screen.laporan.LaporanViewModel
import com.sentral.org.ui.screen.settings.BackupRestoreViewModel
import com.sentral.org.ui.viewmodel.AddPrinterViewModel
import com.sentral.org.backup.service.RestoreService
import com.sentral.org.backup.service.BackupService
import com.sentral.org.backup.crypto.BackupCryptoEngine
import com.sentral.org.backup.db.DatabaseFileSwap
import com.sentral.org.backup.db.DatabaseValidator
import com.sentral.org.backup.db.DatabaseSnapshotter
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule =
    module {
        // 1. Core, Concurrency & Database
        single<PosExecutionLock> {
            PosExecutionLockImpl()
        }
        single {
            createPosDatabase(
                Room
                    .databaseBuilder<PosDatabase>(androidContext(), "pos.db")
                    .setDriver(BundledSQLiteDriver()),
            )
        }
        single<PosWriteService> { RoomTransactionRunner(get(), get()) }
        single { ProductSeeder(get(), get()) }
        single { DatabaseWarmup(get(), get()) }

        // Backup & Restore Core
        single {
            DatabaseSnapshotter(get())
        }
        single {
            DatabaseValidator()
        }
        single {
            DatabaseFileSwap(androidContext(), get())
        }
        single {
            BackupCryptoEngine()
        }
        single {
            BackupService(
                context = androidContext(),
                snapshotter = get(),
                validator = get(),
                crypto = get(),
                lock = get(),
            )
        }
        single {
            RestoreService(
                context = androidContext(),
                validator = get(),
                crypto = get(),
                fileSwap = get(),
                lock = get(),
            )
        }
        viewModel {
            BackupRestoreViewModel(
                backupService = get(),
                restoreService = get(),
                application = androidApplication(),
            )
        }

        // 2. DAOs
        single { get<PosDatabase>().produkDao() }
        single { get<PosDatabase>().persediaanDao() }
        single { get<PosDatabase>().pergerakanPersediaanDao() }
        single { get<PosDatabase>().kasirDao() }
        single { get<PosDatabase>().shiftDao() }
        single { get<PosDatabase>().pergerakanKasDao() }
        single { get<PosDatabase>().keranjangDao() }
        single { get<PosDatabase>().itemKeranjangDao() }
        single { get<PosDatabase>().transaksiDao() }
        single { get<PosDatabase>().itemTransaksiDao() }
        single { get<PosDatabase>().pembayaranDao() }
        single { get<PosDatabase>().returDao() }
        single { get<PosDatabase>().printerDao() }
        single { get<PosDatabase>().profilTokoDao() }

        // 3. Repositories
        single<ProdukRepository> { OfflineProdukRepository(get()) }
        single<PersediaanRepository> { OfflinePersediaanRepository(get(), get()) }
        single<KasirRepository> { OfflineKasirRepository(get()) }
        single<ShiftRepository> { OfflineShiftRepository(get()) }
        single<CartRepository> { OfflineCartRepository(get(), get()) }
        single<TransaksiRepository> { OfflineTransaksiRepository(get(), get(), get()) }
        single<ReturRepository> { OfflineReturRepository(get()) }
        single<PrinterRepository> { OfflinePrinterRepository(get()) }
        single<ProfilTokoRepository> { OfflineProfilTokoRepository(get()) }
        single<LaporanRepository> {
            OfflineLaporanRepository(get(), get())
        }

        // 4. Domain Services
        factory { InventoryMutationService(persediaanDao = get(), ledgerDao = get()) }
        factory { PersediaanService(write = get(), products = get(), stock = get(), ledger = get()) }
        factory { CartService(write = get(), carts = get(), items = get(), products = get(), cashiers = get()) }
        single {
            ProductManagementService(
                write = get(),
                produkDao = get(),
                persediaanDao = get(),
                ledgerDao = get(),
                mutationService = get(),
            )
        }
        factory {
            ShiftService(
                write = get(),
                cashiers = get(),
                shifts = get(),
                cashLedger = get(),
                transactions = get(),
                payments = get(),
            )
        }
        single {
            AuthService(
                kasirDao = get(),
                shiftDao = get(),
                pinHasher = get(),
                rateLimiter = get(),
                sessionProvider = get(),
            )
        }
        factory {
            CheckoutService(
                write = get(),
                products = get(),
                carts = get(),
                cartItems = get(),
                cashiers = get(),
                shifts = get(),
                transactions = get(),
                transactionItems = get(),
                payments = get(),
                cashLedger = get(),
                inventory = get(),
            )
        }
        factory {
            ReturService(
                write = get(),
                transactions = get(),
                transactionItems = get(),
                returns = get(),
                cashiers = get(),
                shifts = get(),
                cashLedger = get(),
                inventory = get(),
            )
        }
        factory {
            VoidService(
                write = get(),
                transactions = get(),
                transactionItems = get(),
                returns = get(),
                cashiers = get(),
                shifts = get(),
                payments = get(),
                cashLedger = get(),
                inventory = get(),
            )
        }

        // 6. Printer & Hardware
        single {
            PrinterService(
                printerDao = get(),
                driverFactory = { printer -> EscPosPrinterDriver(androidContext(), printer) },
                scope =
                    kotlinx.coroutines.CoroutineScope(
                        kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob(),
                    ),
            )
        }

        // 5. Keamanan & Sesi Otoritatif
        single<PinHasher> { createPinHasher() }
        single { PinRateLimiter() }
        single { ActiveSesiKasirProvider(kasirDao = get(), shiftDao = get()) }
        single<SesiKasirProvider> { get<ActiveSesiKasirProvider>() }
        viewModelOf(::MainViewModel)
        viewModel {
            KasirViewModel(
                cartService = get(),
                checkoutService = get(),
                produkRepo = get(),
                cartRepo = get(),
                persediaanDao = get(),
                profilRepo = get(),
                sesi = get(),
                printerService = get(),
                transaksiRepo = get(), // ← BARU
            )
        }
        viewModel {
            AddPrinterViewModel(
                application = androidApplication(),
                printerRepo = get(),
                printerService = get(),
            )
        }
        viewModel {
            PrinterSettingsViewModel(
                printerRepo = get(),
                profilRepo = get(),
            )
        }
        single { ExcelReportExporter(androidContext()) }
        factory { TransaksiExportUseCase(transaksiRepo = get(), excelExporter = get()) }
        viewModel {
            RiwayatViewModel(
                transaksiRepo = get(),
                profilRepo = get(),
                printerService = get(),
                exportUseCase = get(),
                voidService = get(),
                returService = get(),
                returDao = get(),
                sessionProvider = get(),
            )
        }
        viewModel {
            LoginKasirViewModel(
                kasirDao = get(),
                authService = get(),
            )
        }
        viewModel {
            BukaShiftViewModel(
                shiftService = get(),
                kasirDao = get(),
                sessionProvider = get(),
            )
        }
        viewModel {
            TutupShiftViewModel(
                shiftService = get(),
                sessionProvider = get(),
                printerService = get(),
                profilRepo = get(),
            )
        }
        viewModel {
            LaporanViewModel(
                laporanRepo = get(),
            )
        }
        viewModel {
            KelolaProdukViewModel(
                productService = get(),
                sessionProvider = get(),
            )
        }
        viewModel { (handle: androidx.lifecycle.SavedStateHandle) ->
            FormProdukViewModel(
                savedStateHandle = handle,
                productService = get(),
                produkDao = get(),
                sessionProvider = get(),
            )
        }
    }
