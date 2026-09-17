package com.sentral.org.data.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.sentral.org.data.model.StatusShift

@Entity(tableName = "shift", foreignKeys = [
    ForeignKey(entity = KasirEntity::class, parentColumns = ["id"], childColumns = ["kasir_id"], onDelete = ForeignKey.RESTRICT),
], indices = [Index("kasir_id"), Index("status"), Index("dimulai_pada"), Index("ditutup_pada")])
data class ShiftEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "kasir_id") val kasirId: Long,
    @ColumnInfo(name = "nama_kasir") val namaKasir: String,
    val status: StatusShift,
    @ColumnInfo(name = "kas_awal") val kasAwal: Long,
    @ColumnInfo(name = "dimulai_pada") val dimulaiPada: Long,
    @ColumnInfo(name = "kas_diharapkan") val kasDiharapkan: Long?,
    @ColumnInfo(name = "kas_aktual") val kasAktual: Long?,
    @ColumnInfo(name = "selisih_kas") val selisihKas: Long?,
    @ColumnInfo(name = "ditutup_pada") val ditutupPada: Long?,
    val catatan: String,
)
