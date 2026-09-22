package com.sentral.org.ui.screen.pos

import android.database.sqlite.SQLiteConstraintException
import com.sentral.org.data.model.PosDataException

/**
 * Konvensi dua lapis: pesan PosDataException SENGAJA ditulis sebagai kalimat
 * siap-tampil di lapisan domain. Fungsi ini hanya menambah fallback utk
 * exception non-domain, sehingga UI tidak pernah menampilkan detail teknis.
 */
fun Throwable.pesanPengguna(): String = when (this) {
    is PosDataException.ProductInactive ->
        "Produk '$productName' tidak aktif. Hapus dari keranjang untuk melanjutkan."
    is PosDataException.NotFound -> message ?: "Data tidak ditemukan"
    is PosDataException.InvalidState -> message ?: "Keadaan sudah berubah, muat ulang layar"
    is PosDataException.Validation -> message ?: "Masukan tidak valid"
    is PosDataException.InsufficientStock -> message ?: "Stok tidak mencukupi"
    is PosDataException.InsufficientDamagedStock -> message ?: "Stok rusak tidak mencukupi"
    is PosDataException.Duplicate -> message ?: "Data sudah terdaftar"
    is SQLiteConstraintException -> "Perubahan ditolak karena bentrok data. Muat ulang lalu coba lagi."
    else -> "Terjadi kesalahan tak terduga. Silakan coba lagi."
}