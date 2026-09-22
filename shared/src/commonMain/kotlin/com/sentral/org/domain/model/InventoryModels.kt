package com.sentral.org.domain.model

import com.sentral.org.data.model.JenisPergerakanPersediaan

data class CreateProductRequest(
    val nama: String,
    val sku: String,
    val barcode: String?,
    val harga: Long,
    val hargaModal: Long,
    val kategori: String,
    val stokAwalScaled: Long = 0L,
    val operator: String = "Admin",
    val shiftId: Long? = null,
)

data class UpdateProductRequest(
    val id: Long,
    val nama: String,
    val sku: String,
    val barcode: String?,
    val harga: Long,
    val hargaModal: Long,
    val kategori: String,
)

data class RestockRequest(
    val produkId: Long,
    val jumlahScaled: Long,
    val alasan: String,
    val operator: String = "Admin",
    val shiftId: Long? = null,
)

data class StockOpnameRequest(
    val produkId: Long,
    val stokFisikScaled: Long,
    val alasan: String,
    val operator: String = "Admin",
    val shiftId: Long? = null,
)

data class RecordDamageRequest(
    val produkId: Long,
    val jumlahScaled: Long,
    val alasan: String,
    val operator: String = "Admin",
    val shiftId: Long? = null,
)

data class DisposeDamageRequest(
    val produkId: Long,
    val jumlahScaled: Long,
    val alasan: String,
    val operator: String = "Admin",
    val shiftId: Long? = null,
)

data class KartuStokFilter(
    val produkId: Long,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val jenis: JenisPergerakanPersediaan? = null,
)