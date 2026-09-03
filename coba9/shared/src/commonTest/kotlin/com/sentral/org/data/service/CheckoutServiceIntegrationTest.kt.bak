package com.sentral.org.data.service

import com.sentral.org.data.PosDatabase
import com.sentral.org.data.createInMemoryPosDatabase
import com.sentral.org.data.entity.ItemKeranjangEntity
import com.sentral.org.data.entity.KasirEntity
import com.sentral.org.data.entity.KeranjangEntity
import com.sentral.org.data.entity.PersediaanEntity
import com.sentral.org.data.entity.ProdukEntity
import com.sentral.org.data.entity.ShiftEntity
import com.sentral.org.data.entity.TransaksiEntity
import com.sentral.org.data.model.CheckoutRequest
import com.sentral.org.data.model.DiscountInput
import com.sentral.org.data.model.JenisDiskon
import com.sentral.org.data.model.JenisPergerakanKas
import com.sentral.org.data.model.JenisPergerakanPersediaan
import com.sentral.org.data.model.MetodePembayaran
import com.sentral.org.data.model.PaymentRequest
import com.sentral.org.data.model.PosDataException
import com.sentral.org.data.model.StatusKeranjang
import com.sentral.org.data.model.StatusShift
import com.sentral.org.data.model.StatusTransaksi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CheckoutServiceIntegrationTest {

    private lateinit var db: PosDatabase
    private lateinit var checkoutService: CheckoutService

    private val hargaP1 = 15_000L
    private val qtyP1 = 2_000L
    private val hargaP2 = 7_500L
    private val qtyP2 = 1_000L
    private val stokAwal = 10_000L
    private val subtotalEkspektasi = 37_500L
    private val diskonEkspektasi = 2_500L
    private val totalEkspektasi = 35_000L
    private val uangDiterima = 50_000L
    private val kembalianEkspektasi = 15_000L

    private data class Fixture(
        val cartId: Long,
        val cashierId: Long,
        val shiftId: Long,
        val produkIds: List<Long>,
        val stokAwal: Long,
    )

    @BeforeTest
    fun setUp() {
        db = createInMemoryPosDatabase()
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
            inventory = InventoryMutationService(db.persediaanDao(), db.pergerakanPersediaanDao()),
        )
    }

    @AfterTest
    fun tearDown() {
        db.close()
    }

    private suspend fun seedStandar(): Fixture {
        val now = System.currentTimeMillis()
        val cashierId = db.kasirDao().insert(
            KasirEntity(nama = "Kasir A", pinHash = null, aktif = true, dibuatPada = now)
        )
        val shiftId = db.shiftDao().insert(
            ShiftEntity(
                kasirId = cashierId, namaKasir = "Kasir A", status = StatusShift.TERBUKA,
                kasAwal = 100_000, dimulaiPada = now, kasDiharapkan = null, kasAktual = null,
                selisihKas = null, ditutupPada = null, catatan = "",
            )
        )
        val produkIds = mutableListOf<Long>()
        listOf("Kopi Susu" to hargaP1, "Gula Pasir" to hargaP2).forEachIndexed { i, (nama, harga) ->
            val id = db.produkDao().insert(
                ProdukEntity(
                    nama = nama, sku = "SKU-$i", barcode = null, harga = harga,
                    hargaModal = harga / 2, kategori = "umum", aktif = true,
                    dibuatPada = now, diperbaruiPada = now,
                )
            )
            produkIds += id
            db.persediaanDao().insert(PersediaanEntity(produkId = id, jumlah = stokAwal, jumlahRusak = 0, diperbaruiPada = now))
        }
        val cartId = db.keranjangDao().insert(
            KeranjangEntity(
                nama = "Cart 1", status = StatusKeranjang.AKTIF, kasirId = cashierId,
                namaKasir = "Kasir A", dibuatPada = now, diperbaruiPada = now,
                ditahanPada = null, diselesaikanPada = null, dibatalkanPada = null,
            )
        )
        produkIds.forEachIndexed { i, pid ->
            db.itemKeranjangDao().insert(
                ItemKeranjangEntity(
                    keranjangId = cartId, produkId = pid, namaProduk = "P$i",
                    hargaSatuan = 0,
                    jumlah = if (i == 0) qtyP1 else qtyP2,
                    ditambahkanPada = now, diperbaruiPada = now,
                )
            )
        }
        return Fixture(cartId, cashierId, shiftId, produkIds, stokAwal)
    }

    private fun cashRequest(f: Fixture, number: String) = CheckoutRequest(
        cartId = f.cartId, cashierId = f.cashierId, shiftId = f.shiftId,
        payments = listOf(
            PaymentRequest(method = MetodePembayaran.CASH, amount = totalEkspektasi, received = uangDiterima)
        ),
        discount = DiscountInput.Nominal(diskonEkspektasi),
        transactionNumber = number,
        now = System.currentTimeMillis(),
    )

    @Test
    fun checkoutSuksesMenulisSemuaJejakDenganBenar() = runTest {
        val f = seedStandar()
        val hasil = checkoutService.checkout(cashRequest(f, "TRX-001")).getOrThrow()

        assertEquals(totalEkspektasi, hasil.total)
        assertEquals(subtotalEkspektasi, hasil.subtotal)
        assertEquals(diskonEkspektasi, hasil.discount)
        assertEquals(uangDiterima, hasil.paid)
        assertEquals(kembalianEkspektasi, hasil.change)
        assertEquals(kembalianEkspektasi, hasil.paid - hasil.total)

        val tx = db.transaksiDao().getById(hasil.transactionId)
        assertNotNull(tx)
        assertEquals(StatusTransaksi.SELESAI, tx.status)
        assertEquals(totalEkspektasi, tx.total)

        val items = db.itemTransaksiDao().getByTransaction(hasil.transactionId)
        assertEquals(2, items.size)
        assertEquals(hargaP1, items[0].hargaSatuan)
        assertEquals(hargaP2, items[1].hargaSatuan)
        assertEquals(diskonEkspektasi, items.sumOf { it.diskonItem })
        assertTrue(items.all { it.diskonItem >= 0 })

        val bayar = db.pembayaranDao().getByTransaction(hasil.transactionId)
        assertEquals(1, bayar.size)
        assertEquals(uangDiterima, bayar[0].diterima)
        assertEquals(kembalianEkspektasi, bayar[0].kembalian)

        val kasRows = db.pergerakanKasDao().getByShift(f.shiftId)
        assertEquals(1, kasRows.count { it.jenis == JenisPergerakanKas.PENJUALAN })
        assertEquals(totalEkspektasi, kasRows.filter { it.jenis == JenisPergerakanKas.PENJUALAN }.sumOf { it.jumlahDelta })

        assertEquals(f.stokAwal - qtyP1, db.persediaanDao().getByProdukId(f.produkIds[0])!!.jumlah)
        assertEquals(f.stokAwal - qtyP2, db.persediaanDao().getByProdukId(f.produkIds[1])!!.jumlah)
        assertEquals(StatusKeranjang.SELESAI, db.keranjangDao().getById(f.cartId)!!.status)
    }

    @Test
    fun kegagalanDiTengahTransaksiMengembalikanSemuaTulisan() = runTest {
        val f = seedStandar()
        val nomorBentrok = "TRX-DUP"
        val seededTxId = db.transaksiDao().insert(
            TransaksiEntity(
                nomorTransaksi = nomorBentrok, kasirId = f.cashierId, namaKasir = "Kasir A",
                shiftId = f.shiftId, dibuatPada = System.currentTimeMillis(),
                subtotal = 1, diskon = 0, pajak = 0, total = 1,
                jenisDiskon = JenisDiskon.NOMINAL, nilaiDiskon = 0,
                status = StatusTransaksi.SELESAI, dibatalkanPada = null,
                alasanPembatalan = null, adalahTukarGaransi = false,
            )
        )

        val hasil = checkoutService.checkout(cashRequest(f, nomorBentrok))

        assertTrue(hasil.isFailure, "Harus gagal karena bentrokan nomor")
        assertEquals(StatusKeranjang.AKTIF, db.keranjangDao().getById(f.cartId)!!.status)
        assertEquals(2, db.itemKeranjangDao().getByCart(f.cartId).size)
        assertEquals(f.stokAwal, db.persediaanDao().getByProdukId(f.produkIds[0])!!.jumlah)
        assertEquals(f.stokAwal, db.persediaanDao().getByProdukId(f.produkIds[1])!!.jumlah)
        assertTrue(db.pergerakanPersediaanDao().getByProduk(f.produkIds[0]).none {
            it.jenis == JenisPergerakanPersediaan.PENJUALAN
        })
        assertTrue(db.pembayaranDao().getByTransaction(seededTxId).isEmpty())
        assertEquals(
            0, db.pergerakanKasDao().getByShift(f.shiftId).count { it.jenis == JenisPergerakanKas.PENJUALAN }
        )
    }

    @Test
    fun checkoutGandaSerentakHanyaSatuYangBerhasil() = runTest {
        val f = seedStandar()

        val hasil = coroutineScope {
            val a = async(Dispatchers.IO) { checkoutService.checkout(cashRequest(f, "RACE-A")) }
            val b = async(Dispatchers.IO) { checkoutService.checkout(cashRequest(f, "RACE-B")) }
            listOf(a.await(), b.await())
        }

        val sukses = hasil.filter { it.isSuccess }
        val gagal = hasil.filter { it.isFailure }
        assertEquals(1, sukses.size, "tepat satu pemenang")
        assertEquals(1, gagal.size, "tepat satu pecundang")
        assertTrue(
            gagal[0].exceptionOrNull() is PosDataException.InvalidState,
            "pecundang harus ditolak guard status keranjang",
        )

        val pemenang = sukses[0].getOrThrow()
        assertNull(db.transaksiDao().getByNumber(if (pemenang.transactionNumber == "RACE-A") "RACE-B" else "RACE-A"))
        assertEquals(StatusKeranjang.SELESAI, db.keranjangDao().getById(f.cartId)!!.status)

        assertEquals(f.stokAwal - qtyP1, db.persediaanDao().getByProdukId(f.produkIds[0])!!.jumlah)
        assertEquals(f.stokAwal - qtyP2, db.persediaanDao().getByProdukId(f.produkIds[1])!!.jumlah)
        assertEquals(
            1, db.pergerakanPersediaanDao().getByProduk(f.produkIds[0]).count {
                it.jenis == JenisPergerakanPersediaan.PENJUALAN
            }
        )

        assertEquals(1, db.pembayaranDao().getByTransaction(pemenang.transactionId).size)
        assertEquals(
            totalEkspektasi, db.pergerakanKasDao().getByShift(f.shiftId)
                .filter { it.jenis == JenisPergerakanKas.PENJUALAN }.sumOf { it.jumlahDelta }
        )
    }
}