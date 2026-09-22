package com.sentral.org.data.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(tableName = "kasir", indices = [Index("nama", name = "indeks_kasir_nama")])
data class KasirEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nama: String,
    @ColumnInfo(name = "pin_hash") val pinHash: String?,
    val aktif: Boolean,
    @ColumnInfo(name = "dibuat_pada") val dibuatPada: Long,
)
