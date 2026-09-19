package com.sentral.org.data.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.sentral.org.data.model.JenisDiskon
import com.sentral.org.data.model.StatusTransaksi

@Entity(tableName = "transaksi", foreignKeys = [
    ForeignKey(entity = KasirEntity::class, parentColumns = ["id"], childColumns = ["kasir_id"], onDelete = ForeignKey.RESTRICT),
    ForeignKey(entity = ShiftEntity::class, parentColumns = ["id"], childColumns = ["shift_id"], onDelete = ForeignKey.RESTRICT),
], indices = [
    Index(value = ["nomor_transaksi"], unique = true, name = "unik_transaksi_nomor"),
    Index("kasir_id"), Index("shift_id"), Index("dibuat_pada"), Index("status"),
])
data class TransaksiEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "nomor_transaksi") val nomorTransaksi: String,
    @ColumnInfo(name = "kasir_id") val kasirId: Long,
    @ColumnInfo(name = "nama_kasir") val namaKasir: String,
    @ColumnInfo(name = "shift_id") val shiftId: Long,
    @ColumnInfo(name = "dibuat_pada") val dibuatPada: Long,
    val subtotal: Long,
    val diskon: Long,
    val pajak: Long,
    val total: Long,
    @ColumnInfo(name = "jenis_diskon") val jenisDiskon: JenisDiskon,
    @ColumnInfo(name = "nilai_diskon") val nilaiDiskon: Long,
    val status: StatusTransaksi,
    @ColumnInfo(name = "dibatalkan_pada") val dibatalkanPada: Long?,
    @ColumnInfo(name = "alasan_pembatalan") val alasanPembatalan: String?,
    @ColumnInfo(name = "adalah_tukar_garansi") val adalahTukarGaransi: Boolean,
)
