package com.sentral.org.data.service

import com.sentral.org.data.PosDatabase
import com.sentral.org.data.createInMemoryPosDatabase
import com.sentral.org.data.entity.ItemKeranjangEntity
import com.sentral.org.data.entity.KasirEntity
import com.sentral.org.data.entity.KeranjangEntity
import com.sentral.org.data.entity.PergerakanKasEntity
import com.sentral.org.data.entity.PersediaanEntity
import com.sentral.org.data.entity.ProdukEntity
import com.sentral.org.data.entity.ShiftEntity
import com.sentral.org.data.model.CheckoutRequest
import com.sentral.org.data.model.CheckoutResult
import com.sentral.org.data.model.JenisPergerakanKas
import com.sentral.org.data.model.JenisPergerakanPersediaan
import com.sentral.org.data.model.MetodePembayaran
import com.sentral.org.data.model.PaymentRequest
import com.sentral.org.data.model.PosDataException
import com.sentral.org.data.model.ReturnLineRequest
import com.sentral.org.data.model.ReturnRequest
import com.sentral.org.data.model.StatusKeranjang
import com.sentral.org.data.model.StatusShift
import com.sentral.org.data.model.StatusTransaksi
import com.sentral.org.data.model.TujuanStokPengembalian
import com.sentral.org.data.model.VoidRequest
import com.sentral.org.shared.currentTimeMillis
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class VoidServiceIntegrationTest {

    private lateinit var db: PosDatabase
    private lateinit var voidService: VoidService
    private lateinit var checkoutService: CheckoutService
    private lateinit var returService: ReturService

    @BeforeTest
    fun setUp() {
        db = createInMemoryPosDatabase()
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

    @AfterTest
    fun tearDown() {
        db.close()
    }

    private data class Sesi(val kasirId: Long, val shiftId: Long)

    private suspend fun seedKasirShift(): Sesi {
        val now = currentTimeMillis()
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
        db.pergerakanKasDao().insert(
            PergerakanKasEntity(
                shiftId = shiftId, jenis = JenisPergerakanKas.KAS_AWAL, jumlahDelta = 100_000,
                transaksiId = null, pengembalianId = null, keterangan = "Kas awal", dibuatPada = now,
            )
        )
        return Sesi(kasirId, shiftId)
    }

    private suspend fun seedProduk(sku: String, harga: Long, stok: Long): Long {
        val now = currentTimeMillis()
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

    private suspend fun checkoutLangsung(
        sesi: Sesi, produkId: Long, harga: Long, qty: Long, nomor: String,
        payment: PaymentRequest,
    ): CheckoutResult {
        val now = currentTimeMillis()
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

    @Test
    fun voidCashMengembalikanStokMengurangiKasDanMenandaiTransaksi() = runTest {
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
                reason = "Salah input kasir", now = currentTimeMillis(),
            )
        ).getOrThrow()

        val tx = db.transaksiDao().getById(trx.transactionId)!!
        assertEquals(StatusTransaksi.VOID, tx.status)
        assertNotNull(tx.dibatalkanPada)
        assertEquals("Salah input kasir", tx.alasanPembatalan)

        val stok = db.persediaanDao().getByProdukId(produkId)!!
        assertEquals(9_000L, stok.jumlah)
        assertEquals(0L, stok.jumlahRusak)

        val mutasi = db.pergerakanPersediaanDao().getByProduk(produkId)
            .single { it.jenis == JenisPergerakanPersediaan.PEMBATALAN_PENJUALAN }
        assertEquals(2_000L, mutasi.perubahanJumlah)
        assertEquals(7_000L, mutasi.saldoJumlahSebelum)
        assertEquals(9_000L, mutasi.saldoJumlahSetelah)

        val kasRows = db.pergerakanKasDao().getByShift(sesi.shiftId)
        assertEquals(24_000L, kasRows.filter { it.jenis == JenisPergerakanKas.PENJUALAN }.sumOf { it.jumlahDelta })
        val refundRow = kasRows.single { it.jenis == JenisPergerakanKas.RETUR }
        assertEquals(-24_000L, refundRow.jumlahDelta)
        assertEquals(trx.transactionId, refundRow.transaksiId)
        assertEquals(100_000L, db.pergerakanKasDao().getExpectedCash(sesi.shiftId))
    }

    @Test
    fun voidQrisMengembalikanStokTanpaPergerakanKasFisik() = runTest {
        val sesi = seedKasirShift()
        val produkId = seedProduk("VOID-QRIS", harga = 8_000, stok = 5_000)
        val trx = checkoutLangsung(
            sesi, produkId, 8_000, 1_000, "TRX-V2",
            PaymentRequest(MetodePembayaran.QRIS, amount = 8_000),
        )

        voidService.void(
            VoidRequest(
                transactionId = trx.transactionId, cashierId = sesi.kasirId, shiftId = sesi.shiftId,
                reason = "QRIS gagal konfirmasi", now = currentTimeMillis(),
            )
        ).getOrThrow()

        assertEquals(StatusTransaksi.VOID, db.transaksiDao().getById(trx.transactionId)!!.status)
        assertEquals(5_000L, db.persediaanDao().getByProdukId(produkId)!!.jumlah)

        val kasRows = db.pergerakanKasDao().getByShift(sesi.shiftId)
        assertTrue(kasRows.none { it.jenis == JenisPergerakanKas.PENJUALAN })
        assertTrue(kasRows.none { it.jenis == JenisPergerakanKas.RETUR })
        assertEquals(100_000L, db.pergerakanKasDao().getExpectedCash(sesi.shiftId))
    }

    @Test
    fun voidPadaTransaksiYangSudahDireturDitolakTanpaEfek() = runTest {
        val sesi = seedKasirShift()
        val produkId = seedProduk("VOID-AFTER-RET", harga = 12_000, stok = 9_000)
        val trx = checkoutLangsung(
            sesi, produkId, 12_000, 2_000, "TRX-V3",
            PaymentRequest(MetodePembayaran.CASH, amount = 24_000, received = 24_000),
        )
        val itemId = itemIdPertama(trx.transactionId)

        returService.process(
            ReturnRequest(
                transactionId = trx.transactionId, cashierId = sesi.kasirId, shiftId = sesi.shiftId,
                lines = listOf(ReturnLineRequest(itemId, 1_000, TujuanStokPengembalian.NORMAL)),
                refundMethod = MetodePembayaran.QRIS, now = currentTimeMillis(),
            )
        ).getOrThrow()

        val hasil = voidService.void(
            VoidRequest(
                transactionId = trx.transactionId, cashierId = sesi.kasirId, shiftId = sesi.shiftId,
                reason = "harus gagal", now = currentTimeMillis(),
            )
        )

        assertTrue(hasil.isFailure)
        assertTrue(hasil.exceptionOrNull() is PosDataException.InvalidState)

        assertEquals(StatusTransaksi.SELESAI, db.transaksiDao().getById(trx.transactionId)!!.status)
        assertEquals(8_000L, db.persediaanDao().getByProdukId(produkId)!!.jumlah)
        assertTrue(
            db.pergerakanPersediaanDao().getByProduk(produkId)
                .none { it.jenis == JenisPergerakanPersediaan.PEMBATALAN_PENJUALAN }
        )
        assertTrue(
            db.pergerakanKasDao().getByShift(sesi.shiftId).none { it.jenis == JenisPergerakanKas.RETUR }
        )
    }

    @Test
    fun voidGandaHanyaEfekSatuKali() = runTest {
        val sesi = seedKasirShift()
        val produkId = seedProduk("VOID-DOUBLE", harga = 10_000, stok = 10_000)
        val trx = checkoutLangsung(
            sesi, produkId, 10_000, 3_000, "TRX-V4",
            PaymentRequest(MetodePembayaran.CASH, amount = 30_000, received = 30_000),
        )

        voidService.void(
            VoidRequest(
                transactionId = trx.transactionId, cashierId = sesi.kasirId, shiftId = sesi.shiftId,
                reason = "pertama", now = currentTimeMillis(),
            )
        ).getOrThrow()

        val kedua = voidService.void(
            VoidRequest(
                transactionId = trx.transactionId, cashierId = sesi.kasirId, shiftId = sesi.shiftId,
                reason = "kedua harus gagal", now = currentTimeMillis(),
            )
        )

        assertTrue(kedua.isFailure)
        assertTrue(kedua.exceptionOrNull() is PosDataException.InvalidState)

        assertEquals(10_000L, db.persediaanDao().getByProdukId(produkId)!!.jumlah)
        assertEquals(
            1, db.pergerakanPersediaanDao().getByProduk(produkId)
                .count { it.jenis == JenisPergerakanPersediaan.PEMBATALAN_PENJUALAN }
        )
        assertEquals(
            1, db.pergerakanKasDao().getByShift(sesi.shiftId)
                .count { it.jenis == JenisPergerakanKas.RETUR }
        )
        assertEquals(100_000L, db.pergerakanKasDao().getExpectedCash(sesi.shiftId))
    }
}