package com.sentral.org.ui.screen.inventory

data class FormProdukUiState(
    val produkId: Long? = null,
    val isEditMode: Boolean = false,
    val nama: String = "",
    val sku: String = "",
    val barcode: String = "",
    val hargaJualInput: String = "",
    val hargaModalInput: String = "",
    val kategori: String = "",
    val stokAwalInput: String = "0",
    val kategoriTersedia: List<String> = emptyList(),
    val scannerTerbuka: Boolean = false,
    val sedangMenyimpan: Boolean = false,
    val pesanError: String? = null,
)

sealed interface FormProdukEvent {
    data class Pesan(
        val teks: String,
        val isError: Boolean = false,
    ) : FormProdukEvent

    data object SimpanSukses : FormProdukEvent
}
