package com.sentral.org.data.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.sentral.org.data.model.MetodePembayaran

@Entity(tableName = "pengembalian", foreignKeys = [
    ForeignKey(entity = TransaksiEntity::class, parentColumns = ["id"], childColumns = ["transaksi_id"], onDelete = ForeignKey.RESTRICT),
    ForeignKey(entity = TransaksiEntity::class, parentColumns = ["id"], childColumns = ["transaksi_pengganti_id"], onDelete = ForeignKey.SET_NULL),
    ForeignKey(entity = KasirEntity::class, parentColumns = ["id"], childColumns = ["kasir_id"], onDelete = ForeignKey.SET_NULL),
    ForeignKey(entity = ShiftEntity::class, parentColumns = ["id"], childColumns = ["shift_id"], onDelete = ForeignKey.SET_NULL),
], indices = [Index("transaksi_id"), Index("transaksi_pengganti_id"), Index("kasir_id"), Index("shift_id"), Index("dikembalikan_pada")])
data class PengembalianEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "transaksi_id") val transaksiId: Long,
    @ColumnInfo(name = "transaksi_pengganti_id") val transaksiPenggantiId: Long?,
    @ColumnInfo(name = "dikembalikan_pada") val dikembalikanPada: Long,
    @ColumnInfo(name = "kasir_id") val kasirId: Long?,
    @ColumnInfo(name = "shift_id") val shiftId: Long?,
    @ColumnInfo(name = "nama_kasir") val namaKasir: String,
    @ColumnInfo(name = "jumlah_pengembalian") val jumlahPengembalian: Long,
    @ColumnInfo(name = "metode_pengembalian") val metodePengembalian: MetodePembayaran,
    val catatan: String,
    @ColumnInfo(name = "adalah_tukar_garansi") val adalahTukarGaransi: Boolean,
)
