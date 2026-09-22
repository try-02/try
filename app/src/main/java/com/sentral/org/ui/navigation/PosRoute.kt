package com.sentral.org.ui.navigation

import kotlinx.serialization.Serializable

sealed interface PosRoute {
    @Serializable data object Splash : PosRoute

    @Serializable data object LoginKasir : PosRoute

    @Serializable data object BukaShift : PosRoute

    @Serializable data object PosUtama : PosRoute

    @Serializable data object TutupShift : PosRoute

    @Serializable data class RiwayatTransaksi(
        val kasirId: Long? = null,
    ) : PosRoute

    @Serializable data object PrinterSettings : PosRoute

    @Serializable data object AddPrinter : PosRoute

    @Serializable data object BackupRestore : PosRoute

    @Serializable data object KelolaProduk : PosRoute

    @Serializable data class FormProduk(
        val produkId: Long? = null,
    ) : PosRoute
}
