package com.sentral.org.di

import com.sentral.org.data.seed.ProductSeeder
import com.sentral.org.data.session.DevSessionBootstrap
import com.sentral.org.data.DatabaseWarmup
import com.sentral.org.data.PosDatabase
import com.sentral.org.data.PosDatabaseFactory
import com.sentral.org.data.repository.*
import com.sentral.org.data.repository.impl.*
import com.sentral.org.data.service.*
import com.sentral.org.data.session.DevSesiKasirProvider
import com.sentral.org.data.session.SesiKasirProvider
import com.sentral.org.ui.MainViewModel
import com.sentral.org.ui.screen.pos.CheckoutViewModel
import com.sentral.org.ui.screen.pos.KasirViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    // 1. Core & Database
    single { PosDatabaseFactory.create(androidContext()) }
    single<PosWriteService> { RoomTransactionRunner(get()) }
    single { ProductSeeder(get()) }              // tambah baru
    single { DevSessionBootstrap(get(), get(), get()) }   // ShiftService sudah ada sebagai factory
    single { DatabaseWarmup(get(), get(), get()) }      // 3 dependensi

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

    // 4. Domain Services
    factory { InventoryMutationService(persediaanDao = get(), ledgerDao = get()) }
    factory { PersediaanService(write = get(), products = get(), stock = get(), ledger = get()) }
    factory { CartService(write = get(), carts = get(), items = get(), products = get(), cashiers = get()) }
    factory { ShiftService(write = get(), cashiers = get(), shifts = get(), cashLedger = get()) }
    factory {
        CheckoutService(
            write = get(), products = get(), carts = get(), cartItems = get(),
            cashiers = get(), shifts = get(), transactions = get(), transactionItems = get(),
            payments = get(), cashLedger = get(), inventory = get(),
        )
    }
    factory {
        ReturService(
            write = get(), transactions = get(), transactionItems = get(), returns = get(),
            cashiers = get(), shifts = get(), cashLedger = get(), inventory = get(),
        )
    }
    factory {
        VoidService(
            write = get(), transactions = get(), transactionItems = get(), returns = get(),
            cashiers = get(), shifts = get(), payments = get(), cashLedger = get(), inventory = get(),
        )
    }

    // 5. Sesi & ViewModels
    single<SesiKasirProvider> { DevSesiKasirProvider(get(), get()) }
    viewModelOf(::CheckoutViewModel)
    viewModelOf(::MainViewModel)
    viewModelOf(::KasirViewModel)
}