package com.sentral.org.data.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.PrimaryKey
import androidx.room3.Index
import com.sentral.org.data.model.TujuanStokPengembalian

@Entity(
    tableName = "item_pengembalian",
    foreignKeys = [
        ForeignKey(
            entity = PengembalianEntity::class,
            parentColumns = ["id"],
            childColumns = ["pengembalian_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = ItemTransaksiEntity::class,
            parentColumns = ["id"],
            childColumns = ["item_transaksi_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = ProdukEntity::class,
            parentColumns = ["id"],
            childColumns = ["produk_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("pengembalian_id"),
        Index("item_transaksi_id"),
        Index("produk_id"),
        Index(
            value = ["pengembalian_id", "item_transaksi_id"],
            unique = true,
            name = "unik_item_pengembalian"
        )
    ]
)
data class ItemPengembalianEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "tujuan_stok")
    val tujuanStok: TujuanStokPengembalian,

    @ColumnInfo(name = "produk_id")
    val produkId: Long?,

    @ColumnInfo(name = "item_transaksi_id")
    val itemTransaksiId: Long,

    @ColumnInfo(name = "pengembalian_id")
    val pengembalianId: Long,

    @ColumnInfo(name = "nama_produk")
    val namaProduk: String,

    @ColumnInfo(name = "harga_satuan")
    val hargaSatuan: Long,

    @ColumnInfo(name = "jumlah_dikembalikan")
    val jumlahDikembalikan: Long,

    @ColumnInfo(name = "jumlah_refund")
    val jumlahRefund: Long,
)