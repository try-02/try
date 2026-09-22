package com.sentral.org.data.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.sentral.org.data.model.StatusKeranjang

@Entity(
    tableName = "keranjang",
    foreignKeys = [
        ForeignKey(
            entity = KasirEntity::class,
            parentColumns = ["id"],
            childColumns = ["kasir_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("kasir_id"),
        Index(
            value = ["status", "diperbarui_pada"]
        )
    ]
)
data class KeranjangEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nama: String,
    val status: StatusKeranjang,
    @ColumnInfo(name = "kasir_id")
    val kasirId: Long,
    @ColumnInfo(name = "nama_kasir")
    val namaKasir: String,
    @ColumnInfo(name = "dibuat_pada")
    val dibuatPada: Long,
    @ColumnInfo(name = "diperbarui_pada")
    val diperbaruiPada: Long,
    @ColumnInfo(name = "ditahan_pada")
    val ditahanPada: Long?,
    @ColumnInfo(name = "diselesaikan_pada")
    val diselesaikanPada: Long?,
    @ColumnInfo(name = "dibatalkan_pada")
    val dibatalkanPada: Long?,
)
