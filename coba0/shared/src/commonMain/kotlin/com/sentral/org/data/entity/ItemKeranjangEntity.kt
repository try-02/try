package com.sentral.org.data.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

// ItemKeranjangEntity.kt
@Entity(
    tableName = "item_keranjang",
    foreignKeys = [
        ForeignKey(
            entity = KeranjangEntity::class,
            parentColumns = ["id"],
            childColumns = ["keranjang_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProdukEntity::class,
            parentColumns = ["id"],
            childColumns = ["produk_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("keranjang_id"), 
        Index("produk_id"),
        Index(
            value = ["keranjang_id", "produk_id"],
            unique = true,
            name = "unik_item_keranjang_produk"
        )
    ]
)
data class ItemKeranjangEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "keranjang_id")
    val keranjangId: Long,
    @ColumnInfo(name = "produk_id")
    val produkId: Long,
    @ColumnInfo(name = "nama_produk")
    val namaProduk: String,
    @ColumnInfo(name = "harga_satuan")
    val hargaSatuan: Long,
    val jumlah: Long,
    @ColumnInfo(name = "ditambahkan_pada")
    val ditambahkanPada: Long,
    @ColumnInfo(name = "diperbarui_pada")
    val diperbaruiPada: Long,
)
