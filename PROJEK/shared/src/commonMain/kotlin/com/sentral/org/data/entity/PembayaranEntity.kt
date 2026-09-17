package com.sentral.org.data.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.sentral.org.data.model.MetodePembayaran

@Entity(tableName = "pembayaran", foreignKeys = [
    ForeignKey(entity = TransaksiEntity::class, parentColumns = ["id"], childColumns = ["transaksi_id"], onDelete = ForeignKey.RESTRICT),
], indices = [Index("transaksi_id")])
data class PembayaranEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "transaksi_id") val transaksiId: Long,
    val metode: MetodePembayaran,
    val jumlah: Long,
    val diterima: Long?,
    val kembalian: Long?,
    val referensi: String?,
    @ColumnInfo(name = "dibuat_pada") val dibuatPada: Long,
)
