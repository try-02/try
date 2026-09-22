package com.sentral.org.data.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(tableName = "printer", indices = [Index("is_default"), Index("prioritas")])
data class PrinterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nama: String,
    @ColumnInfo(name = "tipe_koneksi") val tipeKoneksi: String,
    @ColumnInfo(name = "is_default") val isDefault: Boolean,
    val prioritas: Int,
    @ColumnInfo(name = "karakter_per_baris") val karakterPerBaris: Int,
    @ColumnInfo(name = "lebar_kertas") val lebarKertas: String,
    @ColumnInfo(name = "mendukung_status") val mendukungStatus: Boolean,
    @ColumnInfo(name = "alamat_bluetooth") val alamatBluetooth: String?,
    @ColumnInfo(name = "alamat_wifi") val alamatWifi: String?,
    @ColumnInfo(name = "port_wifi") val portWifi: Int?,
    @ColumnInfo(name = "usb_vendor_id") val usbVendorId: Int?,
    @ColumnInfo(name = "usb_product_id") val usbProductId: Int?,
    @ColumnInfo(name = "dibuat_pada") val dibuatPada: Long,
    @ColumnInfo(name = "gagal_status_berturut") val gagalStatusBerturut: Int,
    @ColumnInfo(name = "dinonaktifkan_otomatis") val dinonaktifkanOtomatis: Boolean,
)
