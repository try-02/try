package com.sentral.org.data.seed

import com.sentral.org.data.entity.PergerakanPersediaanEntity
import com.sentral.org.data.entity.PersediaanEntity
import com.sentral.org.data.entity.ProdukEntity
import com.sentral.org.data.model.JenisPergerakanPersediaan
import com.sentral.org.data.model.QUANTITY_SCALE
import java.util.Random

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

    /**
     * Data default asli (10 item).
     */
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
            SeedItem("Roti Tawar Sari Roti", "BKY-010", "8991001000101", 18_500, 16_000, "Bakery", stokAwal = 15, rusakAwal = 1),
        )

    /**
     * Menghasilkan produk dummy otomatis dalam jumlah berapapun (default 5.000).
     * Data konsisten (deterministik) berkat seed generator tetap.
     */
    fun generateDummyItems(count: Int = 5000): List<SeedItem> {
        val random = Random(42) // Seed tetap agar data selalu sama tiap kali di-generate

        data class KatInfo(val nama: String, val prefix: String, val barang: List<String>)

        val katalog = listOf(
            KatInfo("Makanan Instan", "MI", listOf("Mie Goreng", "Mie Kuah Kari", "Mie Soto", "Bubur Ayam Instan", "Sup Krim", "Sarden Kaleng", "Bumbu Nasi Goreng", "Bihun Instan")),
            KatInfo("Snack", "SNK", listOf("Keripik Kentang", "Keripik Singkong", "Biskuit Gandum", "Wafer Cokelat", "Kacang Panggang", "Permen Mint", "Popcorn Caramel", "Makaroni Pedas")),
            KatInfo("Minuman", "MNM", listOf("Air Mineral", "Kopi Hitam", "Kopi Susu", "Teh Celup", "Susu UHT", "Jus Apel", "Jus Jeruk", "Minuman Isotonik", "Soda Botol")),
            KatInfo("Sembako", "SMK", listOf("Beras Premium", "Minyak Goreng", "Gula Pasir", "Tepung Terigu", "Garam Beriodium", "Kecap Manis", "Saus Sambal", "Telur Pack")),
            KatInfo("Perawatan Diri", "PWT", listOf("Sabun Cair", "Shampoo Anti Ketombe", "Pasta Gigi", "Sikat Gigi", "Pembersih Wajah", "Deodorant Roll-on", "Hand Body Lotion")),
            KatInfo("Kebersihan", "KBR", listOf("Deterjen Bubuk", "Deterjen Cair", "Pewangi Pakaian", "Sabun Cuci Piring", "Pembersih Lantai", "Karbol Sereh", "Pembersih Kaca")),
            KatInfo("Bakery", "BKY", listOf("Roti Tawar Gandum", "Roti Sobek", "Roti Cokelat", "Roti Keju", "Bolu Kukus", "Kue Kering Nastar", "Pastry Croissant")),
            KatInfo("Kesehatan", "OBT", listOf("Minyak Kayu Putih", "Plester Luka", "Vitamin C 500mg", "Tolak Angin", "Obat Flu & Batuk", "Antiseptik Spray")),
            KatInfo("Alat Tulis", "ATK", listOf("Buku Tulis 58lbr", "Pulpen Gel 0.5", "Pensil 2B", "Penghapus", "Penggaris 30cm", "Kertas HVS A4", "Spidol Marker"))
        )

        val brands = listOf("Sentral", "Prima", "Berkah", "Makmur", "Utama", "Jaya", "Kencana", "Super", "Murni", "Harmoni", "Nusantara", "Sedap", "Segar")
        val variants = listOf("Original", "Spesial", "Ekstra", "Gold", "Classic", "Premium", "Pilihan", "Family Pack", "Eco", "Plus")
        val sizes = listOf("50g", "100g", "250g", "500g", "1kg", "200ml", "330ml", "600ml", "1L", "Pack", "Sachet", "Botol")

        val items = ArrayList<SeedItem>(count)

        for (i in 1..count) {
            val kat = katalog[i % katalog.size]
            val barang = kat.barang[(i / katalog.size) % kat.barang.size]
            val brand = brands[(i * 3) % brands.size]
            val variant = variants[(i * 5) % variants.size]
            val size = sizes[(i * 7) % sizes.size]

            // Contoh: "Mie Goreng Sentral Spesial 100g #105"
            val namaProduk = "$barang $brand $variant $size #$i"

            // SKU: format "MI-00001", "SNK-00002", dst
            val sku = "${kat.prefix}-${String.format("%05d", i)}"

            // Barcode EAN-13 unik Indonesia: awalan "899" diikuti 10 digit terisi (total 13 karakter)
            val barcode = "899${String.format("%010d", i)}"

            // Harga jual kelipatan 500 antara Rp 2.000 s/d Rp 120.000
            val harga = (random.nextInt(237) + 4) * 500L

            // Modal berkisar 70% - 85% dari harga jual (dibulatkan ke ratusan terdekat)
            val marginFactor = 0.70 + (random.nextDouble() * 0.15)
            val hargaModal = ((harga * marginFactor) / 100).toLong() * 100L

            // Stok awal berkisar antara 10 - 250 unit
            val stokAwal = (random.nextInt(241) + 10).toLong()

            // Rusak awal jarang terjadi (~5% peluang)
            val rusakAwal = if (random.nextInt(100) < 5) (random.nextInt(3) + 1).toLong() else 0L

            items.add(
                SeedItem(
                    nama = namaProduk,
                    sku = sku,
                    barcode = barcode,
                    harga = harga,
                    hargaModal = hargaModal,
                    kategori = kat.nama,
                    stokAwal = stokAwal,
                    rusakAwal = rusakAwal
                )
            )
        }

        return items
    }

    /**
     * Menggabungkan 10 default item + dummy items jika dibutuhkan
     */
    fun getAllItems(dummyCount: Int = 5000): List<SeedItem> {
        return getDefaultItems() + generateDummyItems(dummyCount)
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
}