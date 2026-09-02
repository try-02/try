package com.sentral.org.data.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(tableName = "item_transaksi", foreignKeys = [
    ForeignKey(entity = TransaksiEntity::class, parentColumns = ["id"], childColumns = ["transaksi_id"], onDelete = ForeignKey.RESTRICT),
    ForeignKey(entity = ProdukEntity::class, parentColumns = ["id"], childColumns = ["produk_id"], onDelete = ForeignKey.SET_NULL),
], indices = [Index("transaksi_id"), Index("produk_id")])
data class ItemTransaksiEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "transaksi_id") val transaksiId: Long,
    @ColumnInfo(name = "produk_id") val produkId: Long?,
    @ColumnInfo(name = "nama_produk") val namaProduk: String,
    @ColumnInfo(name = "harga_satuan") val hargaSatuan: Long,
    val jumlah: Long,
    @ColumnInfo(name = "total_baris") val totalBaris: Long,
    @ColumnInfo(name = "diskon_item") val diskonItem: Long,
    @ColumnInfo(name = "harga_modal") val hargaModal: Long,
)
