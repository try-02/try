package com.sentral.org.data.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.sentral.org.data.model.JenisPergerakanKas

@Entity(
    tableName = "pergerakan_kas",
    foreignKeys = [
        ForeignKey(
            entity = ShiftEntity::class,
            parentColumns = ["id"],
            childColumns = ["shift_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = TransaksiEntity::class,
            parentColumns = ["id"],
            childColumns = ["transaksi_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = PengembalianEntity::class,
            parentColumns = ["id"],
            childColumns = ["pengembalian_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("shift_id"),
        Index("transaksi_id"),
        Index("pengembalian_id"),
        Index("dibuat_pada")
    ]
)
data class PergerakanKasEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "shift_id")
    val shiftId: Long,
    val jenis: JenisPergerakanKas,
    @ColumnInfo(name = "jumlah_delta")
    val jumlahDelta: Long,
    @ColumnInfo(name = "transaksi_id")
    val transaksiId: Long?,
    @ColumnInfo(name = "pengembalian_id")
    val pengembalianId: Long?,
    val keterangan: String,
    @ColumnInfo(name = "dibuat_pada")
    val dibuatPada: Long,
)
