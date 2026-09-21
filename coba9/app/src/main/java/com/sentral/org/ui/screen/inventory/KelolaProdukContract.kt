package com.sentral.org.ui.screen.inventory

enum class StatusStokFilter(val label: String) {
    SEMUA("Semua"),
    MENIPIS("Menipis (≤ 5)"),
    HABIS("Habis (0)"),
    NONAKTIF("Non-Aktif"),
}

enum class UrutanProduk(val label: String) {
    NAMA_AZ("Nama (A-Z)"),
    NAMA_ZA("Nama (Z-A)"),
    STOK_TERENDAH("Stok Terendah"),
    STOK_TERTINGGI("Stok Tertinggi"),
    HARGA_TERMURAH("Harga Termurah"),
    HARGA_TERMAHAL("Harga Termahal"),
}

data class ProdukItemAdminUi(
    val id: Long,
    val nama: String,
    val sku: String,
    val barcode: String?,
    val harga: Long,
    val hargaModal: Long,
    val kategori: String,
    val stokNormalScaled: Long,
    val stokRusakScaled: Long,
    val aktif: Boolean,
)

data class KelolaProdukUiState(
    val query: String = "",
    val filterStatus: StatusStokFilter = StatusStokFilter.SEMUA,
    val filterKategori: String? = null,
    val urutan: UrutanProduk = UrutanProduk.NAMA_AZ,
    val daftarKategori: List<String> = emptyList(),
    val daftarProduk: List<ProdukItemAdminUi> = emptyList(),
    val sedangMemuat: Boolean = true,
    val dialogPenyesuaianTarget: ProdukItemAdminUi? = null,
    val kartuStokTarget: ProdukItemAdminUi? = null,
)

sealed interface KelolaProdukEvent {
    data class Pesan(val teks: String, val isError: Boolean = false) : KelolaProdukEvent
    data class NavigasiKeForm(val produkId: Long? = null) : KelolaProdukEvent
}