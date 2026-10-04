package com.sentral.org.data.seed

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SeedProductTest {

    // ---------- jumlah ----------

    @Test
    fun getDummyItemsMenghasilkanTotalTarget() {
        assertEquals(SeedProduct.DUMMY_TARGET_COUNT, SeedProduct.getDummyItems().size)
        assertEquals(10, SeedProduct.getDefaultItems().size)
    }

    @Test
    fun getDummyItemsHormatiTotalCountKecil() {
        assertEquals(10, SeedProduct.getDummyItems(totalCount = 10).size)
        assertEquals(15, SeedProduct.getDummyItems(totalCount = 15).size)
    }

    @Test
    fun dummyDeterministikDanReproducible() {
        assertEquals(SeedProduct.getDummyItems(), SeedProduct.getDummyItems())
    }

    // ---------- keunikan SKU & barcode ----------

    @Test
    fun skuDanBarcodeDummySemuaUnik() {
        val items = SeedProduct.getDummyItems()
        assertEquals(items.size, items.map { it.sku }.toSet().size, "ada SKU duplikat")
        assertTrue(items.all { it.barcode != null }, "ada barcode null")
        assertEquals(items.size, items.mapNotNull { it.barcode }.toSet().size, "ada barcode duplikat")
    }

    @Test
    fun dummyMemakaiNamespaceTerpisahDariItemBawaan() {
        val items = SeedProduct.getDummyItems()
        val bawaan = SeedProduct.getDefaultItems()
        val skuBawaan = bawaan.map { it.sku }.toSet()
        val barcodeBawaan = bawaan.mapNotNull { it.barcode }.toSet()
        val dummy = items.drop(bawaan.size)

        assertTrue(dummy.all { it.sku.startsWith("GEN-") }, "ada SKU dummy di luar namespace GEN-")
        assertTrue(dummy.none { it.sku in skuBawaan }, "SKU dummy menabrak SKU bawaan")

        val barcodeDummy = dummy.mapNotNull { it.barcode }
        assertEquals(dummy.size, barcodeDummy.size, "ada barcode dummy yang null")
        assertTrue(barcodeDummy.none { it in barcodeBawaan }, "barcode dummy menabrak barcode bawaan")
        // 10 item pertama persis item bawaan
        assertEquals(bawaan, items.take(bawaan.size))
    }

    // ---------- invarian harga & stok ----------

    @Test
    fun dummyInvarianHargaDanStokWajar() {
        val dummy = SeedProduct.getDummyItems().drop(SeedProduct.getDefaultItems().size)
        assertTrue(dummy.all { it.harga > 0L }, "ada harga <= 0")
        assertTrue(
            dummy.all { it.hargaModal >= 1L && it.hargaModal < it.harga },
            "hargaModal tidak berada di antara 1 dan harga-1",
        )
        assertTrue(dummy.all { it.stokAwal >= 0L }, "ada stok negatif")
        assertTrue(
            dummy.all { it.rusakAwal >= 0 && it.rusakAwal <= it.stokAwal },
            "rusak melebihi stok atau negatif",
        )
        assertTrue(dummy.all { it.kategori.isNotBlank() }, "ada kategori kosong")
        assertTrue(dummy.all { it.nama.isNotBlank() }, "ada nama kosong")
    }

    // ---------- konsistensi mapping entity ----------

    @Test
    fun mappingEntityUkuranKonsistenDenganInput() {
        val items = SeedProduct.getDummyItems(totalCount = 50)
        val waktu = 1_000L
        val ids = (1L..items.size.toLong()).toList()

        val produk = SeedProduct.toProdukEntities(items, waktu)
        val persediaan = SeedProduct.toPersediaanEntities(ids, items, waktu)
        val pergerakan = SeedProduct.toPergerakanEntities(ids, items, waktu)

        assertEquals(items.size, produk.size)
        assertEquals(items.size, persediaan.size)
        assertEquals(items.size, pergerakan.size)
        // saldo ledger ter-skala QUANTITY_SCALE dan konsisten
        val skala = 1_000L
        assertTrue(persediaan.all { it.jumlah % skala == 0L && it.jumlahRusak % skala == 0L })
        assertTrue(
            pergerakan.all {
                it.saldoJumlahSebelum == 0L &&
                    it.saldoJumlahSetelah == it.perubahanJumlah &&
                    it.saldoJumlahSetelah % skala == 0L &&
                    it.saldoRusakSetelah == it.perubahanJumlahRusak
            },
        )
    }
}
