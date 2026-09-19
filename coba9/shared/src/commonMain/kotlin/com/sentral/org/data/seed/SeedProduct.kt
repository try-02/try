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

    fun getDefaultItems(): List<SeedItem> = listOf(
        SeedItem("Indomie Goreng",              "MI-001",  "8991001000019",  3_500,  2_900, "Makanan Instan", stokAwal = 120),
        SeedItem("Chitato Sapi Panggang 68g",   "SNK-002", "8991001000026", 12_000, 10_500, "Snack",          stokAwal = 40),
        SeedItem("Aqua Botol 600ml",            "MNM-003", "8991001000033",  4_000,  3_200, "Minuman",        stokAwal = 96),
        SeedItem("Kopi Kapal Api Special Mix",  "MNM-004", "8991001000040",  2_000,  1_650, "Minuman",        stokAwal = 200),
        SeedItem("Ultra Milk Full Cream 250ml", "MNM-005", "8991001000057",  7_000,  6_200, "Minuman",        stokAwal = 48),
        SeedItem("Beras Pandan Wangi 5kg",      "SMK-006", "8991001000064", 75_000, 68_000, "Sembako",        stokAwal = 25),
        SeedItem("Minyak Goreng Sania 1L",      "SMK-007", "8991001000071", 18_000, 16_800, "Sembako",        stokAwal = 30),
        SeedItem("Sabun Mandi Lifebuoy 75g",    "PWT-008", "8991001000088",  5_000,  4_300, "Perawatan Diri", stokAwal = 60),
        SeedItem("Rinso Anti Noda 770g",        "KBR-009", "8991001000095", 19_500, 17_800, "Kebersihan",     stokAwal = 24),
        SeedItem("Roti Tawar Sari Roti",        "BKY-010", "8991001000101", 18_500, 16_000, "Bakery",         stokAwal = 15, rusakAwal = 1),
    )

    fun toProdukEntities(items: List<SeedItem>, waktu: Long): List<ProdukEntity> =
        items.map {
            ProdukEntity(
                nama = it.nama, sku = it.sku, barcode = it.barcode,
                harga = it.harga, hargaModal = it.hargaModal, kategori = it.kategori,
                aktif = true, dibuatPada = waktu, diperbaruiPada = waktu,
            )
        }

    fun toPersediaanEntities(produkIdByIndex: List<Long>, items: List<SeedItem>, waktu: Long): List<PersediaanEntity> =
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
    fun toPergerakanEntities(produkIdByIndex: List<Long>, items: List<SeedItem>, waktu: Long): List<PergerakanPersediaanEntity> =
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