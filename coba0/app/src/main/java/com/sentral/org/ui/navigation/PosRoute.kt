// ui/navigation/PosRoute.kt
package com.sentral.org.ui.navigation

import kotlinx.serialization.Serializable

sealed interface PosRoute {
    @Serializable data object Splash : PosRoute
    @Serializable data object LoginKasir : PosRoute
    @Serializable data object BukaShift : PosRoute
    @Serializable data object PosUtama : PosRoute
    
    // Contoh mengirim argumen dengan aman
    @Serializable data class RiwayatTransaksi(val kasirId: Long? = null) : PosRoute
}
