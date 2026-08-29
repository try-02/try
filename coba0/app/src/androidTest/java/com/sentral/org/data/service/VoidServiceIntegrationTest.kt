package com.sentral.org.data.service

import androidx.room3.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sentral.org.data.PosDatabase
import com.sentral.org.data.entity.ItemKeranjangEntity
import com.sentral.org.data.entity.KasirEntity
import com.sentral.org.data.entity.KeranjangEntity
import com.sentral.org.data.entity.PergerakanKasEntity
import com.sentral.org.data.entity.PersediaanEntity
import com.sentral.org.data.entity.ProdukEntity
import com.sentral.org.data.entity.ShiftEntity
import com.sentral.org.data.model.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Membuktikan guard VoidService pada database Room sungguhan:
 * pembatalan mengembalikan stok & kas secara SIMETRIS dengan penjualan,
 * transaksi yang sudah diretur tidak bisa di-VOID, dan void ganda mustahil.
 */
@RunWith(AndroidJUnit4::class)
class VoidServiceIntegrationTest {

    private lateinit var db: PosDatabase
    private lateinit var voidService: VoidService
    private lateinit var checkoutService: CheckoutService
    private lateinit var returService: ReturService

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, PosDatabase::class.java).build()
        val inventory = InventoryMutationService(db.persediaanDao(), db.pergerakanPersediaanDao())
        voidService = VoidService(
            write = RoomTransactionRunner(db),
            transactions = db.transaksiDao(),
            transactionItems = db.itemTransaksiDao(),
            returns = db.returDao(),
            cashiers = db.kasirDao(),
            shifts = db.shiftDao(),
            payments = db.pembayaranDao(),
            cashLedger = db.pergerakanKasDao(),
            inventory = inventory,
        )
        checkoutService = CheckoutService(
            write = RoomTransactionRunner(db),
            products = db.produkDao(),
            carts = db.keranjangDao(),
            cartItems = db.itemKeranjangDao(),
            cashiers = db.kasirDao(),
            shifts = db.shiftDao(),
            transactions = db.transaksiDao(),
            transactionItems = db.itemTransaksiDao(),
            payments = db.pembayaranDao(),
            cashLedger = db.pergerakanKasDao(),
            inventory = inventory,
        )
        returService = ReturService(
            write = RoomTransactionRunner(db),
            transactions = db.transaksiDao(),
            transactionItems = db.itemTransaksiDao(),
            returns = db.returDao(),
            cashiers = db.kasirDao(),
            shifts = db.shiftDao(),
            cashLedger = db.pergerakanKasDao(),
            inventory = inventory,
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ---------- Fixture helpers ----------

    private data class Sesi(val kasirId: Long, val shiftId: Long)

    private suspend fun seedKasirShift(): Sesi {
        val now = System.currentTimeMillis()
        val kasirId = db.kasirDao().insert(
            KasirEntity(nama = "Kasir Void", pinHash = null, aktif = true, dibuatPada = now)
        )
        val shiftId = db.shiftDao().insert(
            ShiftEntity(
                kasirId = kasirId, namaKasir = "Kasir Void", status = StatusShift.TERBUKA,
                kasAwal = 100_000, dimulaiPada = now, kasDiharapkan = null, kasAktual = null,
                selisihKas = null, ditutupPada = null, catatan = "",
            )
        )
        // Meniru ShiftService.open(): kas awal WAJIB tercatat di ledger,
        // agar invariant "kas_awal + penjualan - retur = expected cash" teruji penuh.
        db.pergerakanKasDao().insert(
            PergerakanKasEntity(
                shiftId = shiftId, jenis = JenisPergerakanKas.KAS_AWAL, jumlahDelta = 100_000,
                transaksiId = null, pengembalianId = null, keterangan = "Kas awal", dibuatPada = now,
            )
        )
        return Sesi(kasirId, shiftId)
    }

    private suspend fun seedProduk(sku: String, harga: Long, stok: Long): Long {
        val now = System.currentTimeMillis()
        val id = db.produkDao().insert(
            ProdukEntity(
                nama = "Produk $sku", sku = sku, barcode = null, harga = harga,
                hargaModal = harga / 2, kategori = "umum", aktif = true,
                dibuatPada = now, diperbaruiPada = now,
            )
        )
        db.persediaanDao().insert(
            PersediaanEntity(produkId = id, jumlah = stok, jumlahRusak = 0, diperbaruiPada = now)
        )
        return id
    }

    /** Checkout sungguhan lewat jalur produksi (sudah terbukti oleh suite checkout). */
    private suspend fun checkoutLangsung(
        sesi: Sesi, produkId: Long, harga: Long, qty: Long, nomor: String,
        payment: PaymentRequest,
    ): CheckoutResult {
        val now = System.currentTimeMillis()
        val cartId = db.keranjangDao().insert(
            KeranjangEntity(
                nama = "Cart", status = StatusKeranjang.AKTIF, kasirId = sesi.kasirId,
                namaKasir = "Kasir Void", dibuatPada = now, diperbaruiPada = now,
                ditahanPada = null, diselesaikanPada = null, dibatalkanPada = null,
            )
        )
        db.itemKeranjangDao().insert(
            ItemKeranjangEntity(
                keranjangId = cartId, produkId = produkId, namaProduk = "P",
                hargaSatuan = harga, jumlah = qty,
                ditambahkanPada = now, diperbaruiPada = now,
            )
        )
        return checkoutService.checkout(
            CheckoutRequest(
                cartId = cartId, cashierId = sesi.kasirId, shiftId = sesi.shiftId,
                payments = listOf(payment),
                transactionNumber = nomor, now = now,
            )
        ).getOrThrow()
    }

    private suspend fun itemIdPertama(transaksiId: Long): Long =
        db.itemTransaksiDao().getByTransaction(transaksiId).first().id

    // ---------- SKENARIO 1: void CASH end-to-end ----------

    @Test
    fun voidCashMengembalikanStokMengurangiKasDanMenandaiTransaksi() = runBlocking {
        val sesi = seedKasirShift()
        val produkId = seedProduk("VOID-CASH", harga = 12_000, stok = 9_000)
        val trx = checkoutLangsung(
            sesi, produkId, 12_000, 2_000, "TRX-V1",
            PaymentRequest(MetodePembayaran.CASH, amount = 24_000, received = 30_000),
        )
        assertEquals(24_000L, trx.total)
        assertEquals(6_000L, trx.change)

        voidService.void(
            VoidRequest(
                transactionId = trx.transactionId, cashierId = sesi.kasirId, shiftId = sesi.shiftId,
                reason = "Salah input kasir", now = System.currentTimeMillis(),
            )
        ).getOrThrow()

        // Status transaksi & jejak alasan
        val tx = db.transaksiDao().getById(trx.transactionId)!!
        assertEquals(StatusTransaksi.VOID, tx.status)
        assertNotNull(tx.dibatalkanPada)
        assertEquals("Salah input kasir", tx.alasanPembatalan)

        // Stok kembali PERSIS ke saldo awal
        val stok = db.persediaanDao().getByProdukId(produkId)!!
        assertEquals(9_000L, stok.jumlah)
        assertEquals(0L, stok.jumlahRusak)

        // Ledger persediaan: satu mutasi reversal, saldo 7.000 -> 9.000
        val mutasi = db.pergerakanPersediaanDao().getByProduk(produkId)
            .single { it.jenis == JenisPergerakanPersediaan.PEMBATALAN_PENJUALAN }
        assertEquals(2_000L, mutasi.perubahanJumlah)
        assertEquals(7_000L, mutasi.saldoJumlahSebelum)
        assertEquals(9_000L, mutasi.saldoJumlahSetelah)

        // Kas shift: PENJUALAN +24rb lalu RETUR -24rb -> expected kembali ke kas awal
        val kasRows = db.pergerakanKasDao().getByShift(sesi.shiftId)
        assertEquals(24_000L, kasRows.filter { it.jenis == JenisPergerakanKas.PENJUALAN }.sumOf { it.jumlahDelta })
        val refundRow = kasRows.single { it.jenis == JenisPergerakanKas.RETUR }
        assertEquals(-24_000L, refundRow.jumlahDelta)
        assertEquals(trx.transactionId, refundRow.transaksiId)
        assertEquals(100_000L, db.pergerakanKasDao().getExpectedCash(sesi.shiftId))
    }

    // ---------- SKENARIO 2: void QRIS tidak menyentuh kas fisik ----------

    @Test
    fun voidQrisMengembalikanStokTanpaPergerakanKasFisik() = runBlocking {
        val sesi = seedKasirShift()
        val produkId = seedProduk("VOID-QRIS", harga = 8_000, stok = 5_000)
        val trx = checkoutLangsung(
            sesi, produkId, 8_000, 1_000, "TRX-V2",
            PaymentRequest(MetodePembayaran.QRIS, amount = 8_000),
        )

        voidService.void(
            VoidRequest(
                transactionId = trx.transactionId, cashierId = sesi.kasirId, shiftId = sesi.shiftId,
                reason = "QRIS gagal konfirmasi", now = System.currentTimeMillis(),
            )
        ).getOrThrow()

        assertEquals(StatusTransaksi.VOID, db.transaksiDao().getById(trx.transactionId)!!.status)
        assertEquals(5_000L, db.persediaanDao().getByProdukId(produkId)!!.jumlah)

        // Penjualan QRIS tidak mencatat kas fisik, begitu pula refund-nya.
        val kasRows = db.pergerakanKasDao().getByShift(sesi.shiftId)
        assertTrue(kasRows.none { it.jenis == JenisPergerakanKas.PENJUALAN })
        assertTrue(kasRows.none { it.jenis == JenisPergerakanKas.RETUR })
        assertEquals(100_000L, db.pergerakanKasDao().getExpectedCash(sesi.shiftId))
    }

    // ---------- SKENARIO 3: retur memblokir void ----------

    @Test
    fun voidPadaTransaksiYangSudahDireturDitolakTanpaEfek() = runBlocking {
        val sesi = seedKasirShift()
        val produkId = seedProduk("VOID-AFTER-RET", harga = 12_000, stok = 9_000)
        val trx = checkoutLangsung(
            sesi, produkId, 12_000, 2_000, "TRX-V3",
            PaymentRequest(MetodePembayaran.CASH, amount = 24_000, received = 24_000),
        )
        val itemId = itemIdPertama(trx.transactionId)

        // Retur PARSIAL 1 dari 2 unit (refund QRIS agar kas tak berubah).
        returService.process(
            ReturnRequest(
                transactionId = trx.transactionId, cashierId = sesi.kasirId, shiftId = sesi.shiftId,
                lines = listOf(ReturnLineRequest(itemId, 1_000, TujuanStokPengembalian.NORMAL)),
                refundMethod = MetodePembayaran.QRIS, now = System.currentTimeMillis(),
            )
        ).getOrThrow()

        val hasil = voidService.void(
            VoidRequest(
                transactionId = trx.transactionId, cashierId = sesi.kasirId, shiftId = sesi.shiftId,
                reason = "harus gagal", now = System.currentTimeMillis(),
            )
        )

        assertTrue(hasil.isFailure)
        assertTrue(hasil.exceptionOrNull() is PosDataException.InvalidState)

        // Void GAGAL total: transaksi tetap SELESAI, stok hanya terpengaruh retur.
        assertEquals(StatusTransaksi.SELESAI, db.transaksiDao().getById(trx.transactionId)!!.status)
        assertEquals(8_000L, db.persediaanDao().getByProdukId(produkId)!!.jumlah) // 9rb - 2rb + 1rb
        assertTrue(
            db.pergerakanPersediaanDao().getByProduk(produkId)
                .none { it.jenis == JenisPergerakanPersediaan.PEMBATALAN_PENJUALAN }
        )
        assertTrue(
            db.pergerakanKasDao().getByShift(sesi.shiftId).none { it.jenis == JenisPergerakanKas.RETUR }
        )
    }

    // ---------- SKENARIO 4: void ganda mustahil ----------

    @Test
    fun voidGandaHanyaEfekSatuKali() = runBlocking {
        val sesi = seedKasirShift()
        val produkId = seedProduk("VOID-DOUBLE", harga = 10_000, stok = 10_000)
        val trx = checkoutLangsung(
            sesi, produkId, 10_000, 3_000, "TRX-V4",
            PaymentRequest(MetodePembayaran.CASH, amount = 30_000, received = 30_000),
        )

        voidService.void(
            VoidRequest(
                transactionId = trx.transactionId, cashierId = sesi.kasirId, shiftId = sesi.shiftId,
                reason = "pertama", now = System.currentTimeMillis(),
            )
        ).getOrThrow()

        val kedua = voidService.void(
            VoidRequest(
                transactionId = trx.transactionId, cashierId = sesi.kasirId, shiftId = sesi.shiftId,
                reason = "kedua harus gagal", now = System.currentTimeMillis(),
            )
        )

        assertTrue(kedua.isFailure)
        assertTrue(kedua.exceptionOrNull() is PosDataException.InvalidState)

        // Stok dipulihkan TEPAT SEKALI meski dua permintaan void masuk.
        assertEquals(10_000L, db.persediaanDao().getByProdukId(produkId)!!.jumlah)
        assertEquals(
            1, db.pergerakanPersediaanDao().getByProduk(produkId)
                .count { it.jenis == JenisPergerakanPersediaan.PEMBATALAN_PENJUALAN }
        )
        // Refund kas juga hanya sekali.
        assertEquals(
            1, db.pergerakanKasDao().getByShift(sesi.shiftId)
                .count { it.jenis == JenisPergerakanKas.RETUR }
        )
        assertEquals(100_000L, db.pergerakanKasDao().getExpectedCash(sesi.shiftId))
    }
}