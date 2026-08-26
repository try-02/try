package com.sentral.org.data.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "profil_toko")
data class ProfilTokoEntity(
    @PrimaryKey val id: Long = 1,
    @ColumnInfo(name = "nama_toko") val namaToko: String,
    val alamat: String,
    @ColumnInfo(name = "catatan_footer") val catatanFooter: String,
    @ColumnInfo(name = "logo_uri") val logoUri: String?, // Simpan URL absolut atau string URI dari internal storage
    @ColumnInfo(name = "cetak_otomatis") val cetakOtomatis: Boolean,
)
