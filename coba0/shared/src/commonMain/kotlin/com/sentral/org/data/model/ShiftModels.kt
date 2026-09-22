package com.sentral.org.data.model

/**
 * Ringkasan data shift untuk kalkulasi rekonsiliasi kas dan cetak Laporan X/Z.
 */
data class ShiftSummary(
    val shiftId: Long,
    val kasirId: Long,
    val kasirNama: String,
    val dimulaiPada: Long,
    val ditutupPada: Long?,
    val kasAwal: Long,
    val totalPenjualanTunai: Long,
    val totalPenjualanNonTunai: Long,
    val totalReturTunai: Long,
    val kasDiharapkan: Long,
    val kasAktual: Long?,
    val selisihKas: Long?,
    val jumlahTransaksi: Int,
    val isZReport: Boolean, // false = Laporan X (Sementara), true = Laporan Z (Penutupan Final)
)
