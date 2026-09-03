package com.sentral.org.data.service

import androidx.room3.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sentral.org.data.PosDatabase
import com.sentral.org.data.entity.ItemKeranjangEntity
import com.sentral.org.data.entity.ItemTransaksiEntity
import com.sentral.org.data.entity.KasirEntity
import com.sentral.org.data.entity.KeranjangEntity
import com.sentral.org.data.entity.PergerakanKasEntity
import com.sentral.org.data.entity.PersediaanEntity
import com.sentral.org.data.entity.ProdukEntity
import com.sentral.org.data.entity.ShiftEntity
import com.sentral.org.data.entity.TransaksiEntity
import com.sentral.org.data.model.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Membuktikan aturan bisnis ReturService pada database Room sungguhan:
 * arah tujuan stok (NORMAL/RUSAK/DISPOSE), kas shift konsisten, dan yang
 * terpenting: retur bertahap TIDAK PERNAH melebihi nilai neto baris.
 */
@RunWith(AndroidJUnit4::class)
class ReturServiceIntegrationTest {

    private lateinit var db: PosDatabase
    private lateinit var returService: ReturService
    private lateinit var checkoutService: CheckoutService
    private lateinit var voidService: VoidService

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, PosDatabase::class.java).build()
        val inventory = InventoryMutationService(db.persediaanDao(), db.pergerakanPersediaanDao())
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
            KasirEntity(nama = "Kasir Retur", pinHash = null, aktif = true, dibuatPada = now)
        )
        val shiftId = db.shiftDao().insert(
            ShiftEntity(
                kasirId = kasirId, namaKasir = "Kasir Retur", status = StatusShift.TERBUKA,
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

    /** Checkout CASH sungguhan lewat jalur produksi (sudah terbukti oleh suite checkout). */
    private suspend fun checkoutLangsung(
        sesi: Sesi, produkId: Long, harga: Long, qty: Long, nomor: String,
        payment: PaymentRequest,
    ): CheckoutResult {
        val now = System.currentTimeMillis()
        val cartId = db.keranjangDao().insert(
            KeranjangEntity(
                nama = "Cart", status = StatusKeranjang.AKTIF, kasirId = sesi.kasirId,
                namaKasir = "Kasir Retur", dibuatPada = now, diperbaruiPada = now,
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

    /** Penyemaian LANGSUNG transaksi+item dgn nilai baris presisi (untuk tes matematis). */
    private data class TxSeed(val transaksiId: Long, val itemId: Long)

    private suspend fun seedTransaksiPresisi(
        sesi: Sesi, produkId: Long, nomor: String,
        jumlah: Long, totalBaris: Long, diskonItem: Long = 0,
    ): TxSeed {
        val now = System.currentTimeMillis()
        val txId = db.transaksiDao().insert(
            TransaksiEntity(
                nomorTransaksi = nomor, kasirId = sesi.kasirId, namaKasir = "Kasir Retur",
                shiftId = sesi.shiftId, dibuatPada = now,
                subtotal = totalBaris, diskon = diskonItem, pajak = 0,
                total = totalBaris - diskonItem,
                jenisDiskon = JenisDiskon.NOMINAL, nilaiDiskon = diskonItem,
                status = StatusTransaksi.SELESAI, dibatalkanPada = null,
                alasanPembatalan = null, adalahTukarGaransi = false,
            )
        )
        val itemId = db.itemTransaksiDao().insertAll(
            listOf(
                ItemTransaksiEntity(
                    transaksiId = txId, produkId = produkId, namaProduk = "P",
                    hargaSatuan = 0, // tidak dipakai dalam matematika retur
                    jumlah = jumlah, totalBaris = totalBaris,
                    diskonItem = diskonItem, hargaModal = 0,
                )
            )
        ).first()
        return TxSeed(txId, itemId)
    }

    private suspend fun itemIdPertama(transaksiId: Long): Long =
        db.itemTransaksiDao().getByTransaction(transaksiId).first().id

    // ---------- SKENARIO 1: retur NORMAL end-to-end ----------

    @Test
    fun returNormalMengembalikanStokMengurangiKasDanTercatatLengkap() = runBlocking {
        val sesi = seedKasirShift()
        val produkId = seedProduk("RET-NORMAL", harga = 10_000, stok = 10_000)
        val trx = checkoutLangsung(
            sesi, produkId, 10_000, 3_000, "TRX-RN",
            PaymentRequest(MetodePembayaran.CASH, amount = 30_000, received = 30_000),
        )
        assertEquals(30_000L, trx.total)
        val itemId = itemIdPertama(trx.transactionId)

        val hasil = returService.process(
            ReturnRequest(
                transactionId = trx.transactionId, cashierId = sesi.kasirId, shiftId = sesi.shiftId,
                lines = listOf(ReturnLineRequest(itemId, 1_000, TujuanStokPengembalian.NORMAL)),
                refundMethod = MetodePembayaran.CASH, now = System.currentTimeMillis(),
            )
        ).getOrThrow()

        // Refund proporsional eksak: 1/3 x 30.000
        assertEquals(10_000L, hasil.refundAmount)

        // Header & baris pengembalian
        val header = db.returDao().getById(hasil.returnId)!!
        assertEquals(10_000L, header.jumlahPengembalian)
        assertEquals(MetodePembayaran.CASH, header.metodePengembalian)
        val baris = db.returDao().getItemsByReturn(hasil.returnId)
        assertEquals(1, baris.size)
        assertEquals(1_000L, baris[0].jumlahDikembalikan)
        assertEquals(TujuanStokPengembalian.NORMAL, baris[0].tujuanStok)

        // Stok normal bertambah, rusak tetap nol
        val stok = db.persediaanDao().getByProdukId(produkId)!!
        assertEquals(8_000L, stok.jumlah)
        assertEquals(0L, stok.jumlahRusak)

        // Ledger persediaan: saldo 7.000 -> 8.000, tepat satu mutasi
        val mutasi = db.pergerakanPersediaanDao().getByRetur(hasil.returnId).single()
        assertEquals(JenisPergerakanPersediaan.PENGEMBALIAN_NORMAL, mutasi.jenis)
        assertEquals(1_000L, mutasi.perubahanJumlah)
        assertEquals(7_000L, mutasi.saldoJumlahSebelum)
        assertEquals(8_000L, mutasi.saldoJumlahSetelah)

        // Kas shift: KAS_AWAL + PENJUALAN - RETUR = 100rb + 30rb - 10rb
        val returRow = db.pergerakanKasDao().getByShift(sesi.shiftId)
            .single { it.jenis == JenisPergerakanKas.RETUR }
        assertEquals(-10_000L, returRow.jumlahDelta)
        assertEquals(hasil.returnId, returRow.pengembalianId)
        assertEquals(120_000L, db.pergerakanKasDao().getExpectedCash(sesi.shiftId))
    }

    // ---------- SKENARIO 2: retur RUSAK ----------

    @Test
    fun returRusakMasukKeSaldoRusakTanpaMenyentuhStokNormal() = runBlocking {
        val sesi = seedKasirShift()
        val produkId = seedProduk("RET-RUSAK", harga = 5_000, stok = 10_000)
        val trx = checkoutLangsung(
            sesi, produkId, 5_000, 2_000, "TRX-RR",
            PaymentRequest(MetodePembayaran.CASH, amount = 10_000, received = 10_000),
        )
        val itemId = itemIdPertama(trx.transactionId)

        val hasil = returService.process(
            ReturnRequest(
                transactionId = trx.transactionId, cashierId = sesi.kasirId, shiftId = sesi.shiftId,
                lines = listOf(ReturnLineRequest(itemId, 1_000, TujuanStokPengembalian.RUSAK)),
                refundMethod = MetodePembayaran.CASH, now = System.currentTimeMillis(),
            )
        ).getOrThrow()

        assertEquals(5_000L, hasil.refundAmount)
        val stok = db.persediaanDao().getByProdukId(produkId)!!
        assertEquals(8_000L, stok.jumlah)      // barang rusak TIDAK jadi stok normal
        assertEquals(1_000L, stok.jumlahRusak) // masuk keranjang rusak

        val mutasi = db.pergerakanPersediaanDao().getByRetur(hasil.returnId).single()
        assertEquals(JenisPergerakanPersediaan.PENGEMBALIAN_RUSAK, mutasi.jenis)
        assertEquals(0L, mutasi.perubahanJumlah)
        assertEquals(1_000L, mutasi.perubahanJumlahRusak)
        assertEquals(
            -5_000L,
            db.pergerakanKasDao().getByShift(sesi.shiftId)
                .single { it.jenis == JenisPergerakanKas.RETUR }.jumlahDelta
        )
    }

    // ---------- SKENARIO 3: TIDAK_DIKEMBALIKAN ----------

    @Test
    fun returDisposeTidakMengubahPersediaanMaupunKasFisik() = runBlocking {
        val sesi = seedKasirShift()
        val produkId = seedProduk("RET-DISPOSE", harga = 7_000, stok = 5_000)
        val trx = checkoutLangsung(
            sesi, produkId, 7_000, 1_000, "TRX-RD",
            PaymentRequest(MetodePembayaran.CASH, amount = 7_000, received = 7_000),
        )
        val itemId = itemIdPertama(trx.transactionId)

        val hasil = returService.process(
            ReturnRequest(
                transactionId = trx.transactionId, cashierId = sesi.kasirId, shiftId = sesi.shiftId,
                lines = listOf(ReturnLineRequest(itemId, 1_000, TujuanStokPengembalian.TIDAK_DIKEMBALIKAN)),
                refundMethod = MetodePembayaran.QRIS, // uang kembali via QRIS, kas fisik tak tersentuh
                now = System.currentTimeMillis(),
            )
        ).getOrThrow()

        assertEquals(7_000L, hasil.refundAmount)
        // Barang tidak kembali ke mana pun
        val stok = db.persediaanDao().getByProdukId(produkId)!!
        assertEquals(4_000L, stok.jumlah)
        assertEquals(0L, stok.jumlahRusak)
        assertTrue(db.pergerakanPersediaanDao().getByRetur(hasil.returnId).isEmpty())
        // Refund QRIS tidak menyentuh kas fisik
        assertTrue(
            db.pergerakanKasDao().getByShift(sesi.shiftId)
                .none { it.jenis == JenisPergerakanKas.RETUR }
        )
        // Namun tetap tercatat sebagai pengembalian sah
        assertEquals(7_000L, db.returDao().getById(hasil.returnId)!!.jumlahPengembalian)
    }

    // ---------- SKENARIO 4: REGRESI anti-over-refund (paling penting) ----------

    @Test
    fun returBertahapTidakPernahMelebihiNilaiNetoBaris() = runBlocking {
        val sesi = seedKasirShift()
        val produkId = seedProduk("RET-BUDGET", harga = 101, stok = 10_000)
        // Baris disemai langsung dgn nilai presisi: 3 unit bernilai neto Rp101.
        // Pembulatan per-sesi: round(101/3) = 34 -> TANPA klem, 3 sesi = 102 (over-refund!).
        val seed = seedTransaksiPresisi(
            sesi, produkId, "TRX-OVR", jumlah = 3_000, totalBaris = 101,
        )

        suspend fun sesiRetur(): ReturnResult = returService.process(
            ReturnRequest(
                transactionId = seed.transaksiId, cashierId = sesi.kasirId, shiftId = sesi.shiftId,
                lines = listOf(ReturnLineRequest(seed.itemId, 1_000, TujuanStokPengembalian.NORMAL)),
                refundMethod = MetodePembayaran.QRIS, now = System.currentTimeMillis(),
            )
        ).getOrThrow()

        val s1 = sesiRetur()
        val s2 = sesiRetur()
        val s3 = sesiRetur()

        assertEquals(34L, s1.refundAmount)
        assertEquals(34L, s2.refundAmount)
        assertEquals("sesi ke-3 harus diklem oleh sisa budget", 33L, s3.refundAmount)

        // Invariant utama lintas semua sesi: total refund PERSIS neto baris.
        val totalRefund = listOf(s1, s2, s3).sumOf {
            db.returDao().getItemsByReturn(it.returnId).sumOf { b -> b.jumlahRefund }
        }
        assertEquals(101L, totalRefund)

        // Ketiga unit kembali ke stok normal, masing-masing tepat sekali.
        assertEquals(13_000L, db.persediaanDao().getByProdukId(produkId)!!.jumlah)

        // Quantity habis -> sesi ke-4 ditolak guard sisa quantity.
        val s4 = returService.process(
            ReturnRequest(
                transactionId = seed.transaksiId, cashierId = sesi.kasirId, shiftId = sesi.shiftId,
                lines = listOf(ReturnLineRequest(seed.itemId, 1_000, TujuanStokPengembalian.NORMAL)),
                refundMethod = MetodePembayaran.QRIS, now = System.currentTimeMillis(),
            )
        )
        assertTrue(s4.isFailure)
    }

    // ---------- SKENARIO 5: guard quantity ----------

    @Test
    fun returMelebihiSisaQuantityDitolakTanpaJejak() = runBlocking {
        val sesi = seedKasirShift()
        val produkId = seedProduk("RET-GUARD", harga = 10_000, stok = 10_000)
        val seed = seedTransaksiPresisi(sesi, produkId, "TRX-QG", jumlah = 2_000, totalBaris = 20_000)

        val hasil = returService.process(
            ReturnRequest(
                transactionId = seed.transaksiId, cashierId = sesi.kasirId, shiftId = sesi.shiftId,
                lines = listOf(ReturnLineRequest(seed.itemId, 3_000, TujuanStokPengembalian.NORMAL)),
                refundMethod = MetodePembayaran.QRIS, now = System.currentTimeMillis(),
            )
        )

        assertTrue(hasil.isFailure)
        assertTrue(hasil.exceptionOrNull() is PosDataException.Validation)
        // Tidak ada jejak penulisan apa pun
        assertTrue(db.returDao().getByTransaction(seed.transaksiId).isEmpty())
        assertEquals(10_000L, db.persediaanDao().getByProdukId(produkId)!!.jumlah)
    }

    // ---------- SKENARIO 6: retur pada transaksi VOID ----------

    @Test
    fun returPadaTransaksiVoidDitolak() = runBlocking {
        val sesi = seedKasirShift()
        val produkId = seedProduk("RET-VOID", harga = 9_000, stok = 10_000)
        val trx = checkoutLangsung(
            sesi, produkId, 9_000, 1_000, "TRX-RV",
            PaymentRequest(MetodePembayaran.CASH, amount = 9_000, received = 9_000),
        )
        val itemId = itemIdPertama(trx.transactionId)

        voidService.void(
            VoidRequest(trx.transactionId, sesi.kasirId, sesi.shiftId, "tes", System.currentTimeMillis())
        ).getOrThrow()

        val hasil = returService.process(
            ReturnRequest(
                transactionId = trx.transactionId, cashierId = sesi.kasirId, shiftId = sesi.shiftId,
                lines = listOf(ReturnLineRequest(itemId, 1_000, TujuanStokPengembalian.NORMAL)),
                refundMethod = MetodePembayaran.QRIS, now = System.currentTimeMillis(),
            )
        )
        assertTrue(hasil.isFailure)
        assertTrue(hasil.exceptionOrNull() is PosDataException.InvalidState)
    }
}