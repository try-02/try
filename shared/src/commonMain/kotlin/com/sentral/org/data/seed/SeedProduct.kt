package com.sentral.org.data.seed

import com.sentral.org.data.entity.PergerakanPersediaanEntity
import com.sentral.org.data.entity.PersediaanEntity
import com.sentral.org.data.entity.ProdukEntity
import com.sentral.org.data.model.JenisPergerakanPersediaan
import com.sentral.org.data.model.QUANTITY_SCALE

data class SeedItem(
    val nama: String,
    val sku: String,
    val barcode: String?,
    val harga: Long,
    val hargaModal: Long,
    val kategori: String,
    val stokAwal: Long,
    val rusakAwal: Long = 0,
)

object SeedProduct {
    /** Total target produk dummy yang di-seed (termasuk 10 item bawaan). */
    const val DUMMY_TARGET_COUNT = 5_000

    fun getDefaultItems(): List<SeedItem> =
        listOf(
            SeedItem("Indomie Goreng", "MI-001", "8991001000019", 3_500, 2_900, "Makanan Instan", stokAwal = 120),
            SeedItem("Chitato Sapi Panggang 68g", "SNK-002", "8991001000026", 12_000, 10_500, "Snack", stokAwal = 40),
            SeedItem("Aqua Botol 600ml", "MNM-003", "8991001000033", 4_000, 3_200, "Minuman", stokAwal = 96),
            SeedItem("Kopi Kapal Api Special Mix", "MNM-004", "8991001000040", 2_000, 1_650, "Minuman", stokAwal = 200),
            SeedItem("Ultra Milk Full Cream 250ml", "MNM-005", "8991001000057", 7_000, 6_200, "Minuman", stokAwal = 48),
            SeedItem("Beras Pandan Wangi 5kg", "SMK-006", "8991001000064", 75_000, 68_000, "Sembako", stokAwal = 25),
            SeedItem("Minyak Goreng Sania 1L", "SMK-007", "8991001000071", 18_000, 16_800, "Sembako", stokAwal = 30),
            SeedItem("Sabun Mandi Lifebuoy 75g", "PWT-008", "8991001000088", 5_000, 4_300, "Perawatan Diri", stokAwal = 60),
            SeedItem("Rinso Anti Noda 770g", "KBR-009", "8991001000095", 19_500, 17_800, "Kebersihan", stokAwal = 24),
            SeedItem(
                "Roti Tawar Sari Roti",
                "BKY-010",
                "8991001000101",
                18_500,
                16_000,
                "Bakery",
                stokAwal = 15,
                rusakAwal = 1,
            ),
        )

    /**
     * Item bawaan + produk dummy deterministik hingga total [totalCount].
     * Dummy memakai namespace SKU `GEN-` dan prefix barcode `899200` agar tidak
     * pernah menabrak SKU/barcode bawaan maupun data user (unique index aman).
     */
    fun getDummyItems(totalCount: Int = DUMMY_TARGET_COUNT): List<SeedItem> {
        val dasar = getDefaultItems()
        val sisa = (totalCount - dasar.size).coerceAtLeast(0)
        return if (sisa == 0) dasar else dasar + generateDummyItems(sisa)
    }

    private fun generateDummyItems(count: Int): List<SeedItem> =
        List(count) { i ->
            val kategori = KATEGORI[i % KATEGORI.size]
            val urutKategori = i / KATEGORI.size
            val namaDasar = kategori.namaDasar[urutKategori % kategori.namaDasar.size]
            val varian = VARIAN[(urutKategori / kategori.namaDasar.size) % VARIAN.size]
            val hargaDasar = kategori.hargaDasar[(urutKategori + 2) % kategori.hargaDasar.size]
            val margin = ((i % 5) + 1) * 500L
            val harga = hargaDasar + margin
            val serial = (i + 1).toString()
            SeedItem(
                nama = "$namaDasar $varian #$serial",
                sku = "GEN-${kategori.kode}-${serial.padStart(5, '0')}",
                barcode = "899200${serial.padStart(7, '0')}",
                harga = harga,
                hargaModal = maxOf(hargaDasar - margin / 2, 100L),
                kategori = kategori.nama,
                stokAwal = 10L + (i % 190),
                rusakAwal = if (i % 17 == 0) ((i / 17) % 5 + 1).toLong() else 0L,
            )
        }

    fun toProdukEntities(
        items: List<SeedItem>,
        waktu: Long,
    ): List<ProdukEntity> =
        items.map {
            ProdukEntity(
                nama = it.nama,
                sku = it.sku,
                barcode = it.barcode,
                harga = it.harga,
                hargaModal = it.hargaModal,
                kategori = it.kategori,
                aktif = true,
                dibuatPada = waktu,
                diperbaruiPada = waktu,
            )
        }

    fun toPersediaanEntities(
        produkIdByIndex: List<Long>,
        items: List<SeedItem>,
        waktu: Long,
    ): List<PersediaanEntity> =
        items.mapIndexed { i, item ->
            val stokScaled = item.stokAwal * QUANTITY_SCALE
            val rusakScaled = item.rusakAwal * QUANTITY_SCALE
            PersediaanEntity(
                produkId = produkIdByIndex[i],
                jumlah = stokScaled,
                jumlahRusak = rusakScaled,
                diperbaruiPada = waktu,
            )
        }

    /**
     * Ledger "stok awal" per produk dengan saldo ter-skala QUANTITY_SCALE konsisten.
     */
    fun toPergerakanEntities(
        produkIdByIndex: List<Long>,
        items: List<SeedItem>,
        waktu: Long,
    ): List<PergerakanPersediaanEntity> =
        items.mapIndexed { i, item ->
            val stokScaled = item.stokAwal * QUANTITY_SCALE
            val rusakScaled = item.rusakAwal * QUANTITY_SCALE
            PergerakanPersediaanEntity(
                produkId = produkIdByIndex[i],
                jenis = JenisPergerakanPersediaan.STOK_AWAL,
                perubahanJumlah = stokScaled,
                perubahanJumlahRusak = rusakScaled,
                saldoJumlahSebelum = 0L,
                saldoJumlahSetelah = stokScaled,
                saldoRusakSebelum = 0L,
                saldoRusakSetelah = rusakScaled,
                transaksiId = null,
                itemTransaksiId = null,
                pengembalianId = null,
                itemPengembalianId = null,
                shiftId = null,
                keterangan = "Stok awal",
                dibuatPada = waktu,
            )
        }

    private data class KategoriSeed(
        val kode: String,
        val nama: String,
        val namaDasar: List<String>,
        val hargaDasar: List<Long>,
    )

    private val KATEGORI =
        listOf(
            KategoriSeed(
                "MI",
                "Makanan Instan",
                listOf("Mie Instan Goreng", "Mie Instan Kuah", "Bihun Instan", "Mie Instan Kari", "Mie Lontong"),
                listOf(3_000, 3_500, 4_000, 4_500, 5_000),
            ),
            KategoriSeed(
                "SNK",
                "Snack",
                listOf("Keripik Kentang", "Keripik Singkong", "Wafer Cokelat", "Biskuit Kelapa", "Kacang Garing"),
                listOf(6_500, 8_000, 10_000, 12_000, 15_000),
            ),
            KategoriSeed(
                "MNM",
                "Minuman",
                listOf("Teh Kotak", "Air Mineral", "Susu UHT", "Kopi Instan", "Minuman Sirup"),
                listOf(2_000, 4_000, 5_500, 7_000, 15_000),
            ),
            KategoriSeed(
                "SMK",
                "Sembako",
                listOf("Beras Premium", "Gula Pasir", "Minyak Goreng", "Tepung Terigu", "Garam Dapur"),
                listOf(13_000, 18_000, 28_000, 45_000, 75_000),
            ),
            KategoriSeed(
                "PWT",
                "Perawatan Diri",
                listOf("Sabun Mandi", "Sampo", "Pasta Gigi", "Tisu Wajah", "Deodoran"),
                listOf(5_000, 12_000, 18_000, 22_000, 25_000),
            ),
            KategoriSeed(
                "KBR",
                "Kebersihan",
                listOf("Deterjen Bubuk", "Sabun Cuci Piring", "Pembersih Lantai", "Pewangi Ruangan", "Sabun Cuci Tangan"),
                listOf(8_500, 12_000, 15_000, 17_000, 19_500),
            ),
            KategoriSeed(
                "BKY",
                "Bakery",
                listOf("Roti Tawar", "Roti Manis", "Kue Kering", "Bolu Kukus", "Donat Gandum"),
                listOf(8_000, 10_000, 12_000, 18_500, 25_000),
            ),
            KategoriSeed(
                "FRZ",
                "Frozen Food",
                listOf("Nugget Ayam", "Sosis Ayam", "Bakso Sapi", "Dimsum Ayam", "Kentang Goreng Beku"),
                listOf(22_000, 25_000, 30_000, 35_000, 45_000),
            ),
        )

    /** Kualifikasi marketing netral yang cocok dipasangkan dengan nama produk apa pun. */
    private val VARIAN =
        listOf(
            "Original",
            "Spesial",
            "Ekonomis",
            "Premium",
            "Jumbo",
            "Mini",
            "Favorit",
            "Pilihan",
            "Best Seller",
            "Baru",
        )
}
