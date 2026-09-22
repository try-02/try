package com.sentral.org.domain.service

import com.sentral.org.data.PosDatabase
import com.sentral.org.data.createInMemoryPosDatabase
import com.sentral.org.data.model.JenisPergerakanPersediaan
import com.sentral.org.data.model.PosDataException
import com.sentral.org.data.service.InventoryMutationService
import com.sentral.org.data.service.RoomTransactionRunner
import com.sentral.org.domain.model.CreateProductRequest
import com.sentral.org.domain.model.DisposeDamageRequest
import com.sentral.org.domain.model.RecordDamageRequest
import com.sentral.org.domain.model.RestockRequest
import com.sentral.org.domain.model.StockOpnameRequest
import com.sentral.org.domain.model.UpdateProductRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ProductManagementServiceTest {

    private lateinit var db: PosDatabase
    private lateinit var service: ProductManagementService

    @BeforeTest
    fun setUp() {
        db = createInMemoryPosDatabase()
        val writeService = RoomTransactionRunner(db)
        val mutationService = InventoryMutationService(db.persediaanDao(), db.pergerakanPersediaanDao())
        service = ProductManagementService(
            write = writeService,
            produkDao = db.produkDao(),
            persediaanDao = db.persediaanDao(),
            ledgerDao = db.pergerakanPersediaanDao(),
            mutationService = mutationService,
        )
    }

    @AfterTest
    fun tearDown() {
        db.close()
    }

    @Test
    fun createProductSuksesDenganStokAwalDanBukuBesar() = runTest {
        val now = 1000L
        val req = CreateProductRequest(
            nama = "Beras Rojolele 5kg",
            sku = "BRS-001",
            barcode = "8991234567890",
            harga = 75_000,
            hargaModal = 65_000,
            kategori = "Sembako",
            stokAwalScaled = 10_000L, // 10 unit
            operator = "Kasir A",
        )

        val id = service.createProduct(req, now).getOrThrow()
        assertTrue(id > 0)

        val produk = db.produkDao().getById(id)
        assertNotNull(produk)
        assertEquals("Beras Rojolele 5kg", produk.nama)
        assertTrue(produk.aktif)

        val stok = db.persediaanDao().getByProdukId(id)
        assertNotNull(stok)
        assertEquals(10_000L, stok.jumlah)
        assertEquals(0L, stok.jumlahRusak)

        val ledger = db.pergerakanPersediaanDao().getByProduk(id)
        assertEquals(1, ledger.size)
        assertEquals(JenisPergerakanPersediaan.STOK_AWAL, ledger[0].jenis)
        assertEquals(10_000L, ledger[0].perubahanJumlah)
        assertEquals(10_000L, ledger[0].saldoJumlahSetelah)
        assertTrue(ledger[0].keterangan.contains("Kasir A"))
    }

    @Test
    fun createProductGagalBilaSkuDuplikat() = runTest {
        val req1 = CreateProductRequest(
            nama = "Produk A", sku = "SKU-DUP", barcode = null, harga = 1000, hargaModal = 500, kategori = "Umum"
        )
        val req2 = CreateProductRequest(
            nama = "Produk B", sku = "SKU-DUP", barcode = null, harga = 2000, hargaModal = 1000, kategori = "Umum"
        )

        service.createProduct(req1, 1000L).getOrThrow()
        val result = service.createProduct(req2, 1001L)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is PosDataException.Duplicate)
    }

    @Test
    fun deactiveDanActivateMengubahStatusProdukTanpaHapusData() = runTest {
        val req = CreateProductRequest(
            nama = "Produk Soft Delete", sku = "SKU-SD", barcode = null, harga = 1000, hargaModal = 500, kategori = "Umum"
        )
        val id = service.createProduct(req, 1000L).getOrThrow()

        service.deactivateProduct(id, 2000L).getOrThrow()
        assertFalse(db.produkDao().getById(id)!!.aktif)

        service.activateProduct(id, 3000L).getOrThrow()
        assertTrue(db.produkDao().getById(id)!!.aktif)
    }

    @Test
    fun restockMenambahStokDanMencatatLedger() = runTest {
        val id = service.createProduct(
            CreateProductRequest("Produk C", "SKU-C", null, 1000, 500, "Umum", 5_000L), 1000L
        ).getOrThrow()

        service.restock(
            RestockRequest(id, 3_000L, "Faktur INV-01", "Gudang"), 2000L
        ).getOrThrow()

        val stok = db.persediaanDao().getByProdukId(id)!!
        assertEquals(8_000L, stok.jumlah)

        val mutasi = db.pergerakanPersediaanDao().getByProduk(id).first { it.jenis == JenisPergerakanPersediaan.STOK_MASUK }
        assertEquals(3_000L, mutasi.perubahanJumlah)
        assertEquals(5_000L, mutasi.saldoJumlahSebelum)
        assertEquals(8_000L, mutasi.saldoJumlahSetelah)
        assertTrue(mutasi.keterangan.contains("Faktur INV-01"))
    }

    @Test
    fun opnameMenghitungSelisihDanMenyesuaikanSaldo() = runTest {
        val id = service.createProduct(
            CreateProductRequest("Produk D", "SKU-D", null, 1000, 500, "Umum", 10_000L), 1000L
        ).getOrThrow()

        // Opname fisik 8 unit (selisih -2 unit)
        service.adjustStockOpname(
            StockOpnameRequest(id, 8_000L, "Fisik rak kurang 2", "Auditor"), 2000L
        ).getOrThrow()

        val stok = db.persediaanDao().getByProdukId(id)!!
        assertEquals(8_000L, stok.jumlah)

        val mutasi = db.pergerakanPersediaanDao().getByProduk(id).first { it.jenis == JenisPergerakanPersediaan.PENYESUAIAN }
        assertEquals(-2_000L, mutasi.perubahanJumlah)
        assertEquals(10_000L, mutasi.saldoJumlahSebelum)
        assertEquals(8_000L, mutasi.saldoJumlahSetelah)
    }

    @Test
    fun mutasiBarangRusakDanPemusnahanMemindahkanSaldoSecaraSimetris() = runTest {
        val id = service.createProduct(
            CreateProductRequest("Produk Telur", "SKU-E", null, 2000, 1500, "Umum", 10_000L), 1000L
        ).getOrThrow()

        // 1. Catat 2 telur pecah
        service.recordDamage(
            RecordDamageRequest(id, 2_000L, "Pecah saat display", "Staff"), 2000L
        ).getOrThrow()

        val stokSetelahRusak = db.persediaanDao().getByProdukId(id)!!
        assertEquals(8_000L, stokSetelahRusak.jumlah)
        assertEquals(2_000L, stokSetelahRusak.jumlahRusak)

        // 2. Musnahkan 2 telur pecah dari gudang retur
        service.disposeDamage(
            DisposeDamageRequest(id, 2_000L, "Dibuang ke limbah organik", "Supervisor"), 3000L
        ).getOrThrow()

        val stokAkhir = db.persediaanDao().getByProdukId(id)!!
        assertEquals(8_000L, stokAkhir.jumlah)
        assertEquals(0L, stokAkhir.jumlahRusak)

        val kartuStok = db.pergerakanPersediaanDao().getByProduk(id)
        assertTrue(kartuStok.any { it.jenis == JenisPergerakanPersediaan.KERUSAKAN && it.perubahanJumlah == -2_000L && it.perubahanJumlahRusak == 2_000L })
        assertTrue(kartuStok.any { it.jenis == JenisPergerakanPersediaan.PEMUSNAHAN && it.perubahanJumlah == 0L && it.perubahanJumlahRusak == -2_000L })
    }

    @Test
    fun mutasiDitolakJikaMenghasilkanStokNegatif() = runTest {
        val id = service.createProduct(
            CreateProductRequest("Barang Terbatas", "SKU-F", null, 5000, 4000, "Umum", 2_000L), 1000L
        ).getOrThrow()

        // Mencoba mencatat 3 unit rusak padahal hanya ada 2 unit
        val result = service.recordDamage(
            RecordDamageRequest(id, 3_000L, "Harus ditolak", "Staff"), 2000L
        )

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is PosDataException.InsufficientStock)

        // Saldo tetap aman di 2_000L
        assertEquals(2_000L, db.persediaanDao().getByProdukId(id)!!.jumlah)
    }
}