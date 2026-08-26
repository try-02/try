package com.sentral.org.data.session

import com.sentral.org.data.dao.KasirDao
import com.sentral.org.data.dao.ShiftDao
import com.sentral.org.data.entity.KasirEntity
import com.sentral.org.data.service.ShiftService
import kotlinx.coroutines.flow.first

/**
 * SEMENTARA (dev): menjamin ada kasir aktif + shift TERBUKA saat startup,
 * agar alur keranjang & checkout bisa diuji sebelum fitur login PIN dan
 * manajemen shift dibuat. HAPUS file ini + wiring-nya saat fitur tersebut jadi.
 */
class DevSessionBootstrap(
    private val cashiers: KasirDao,
    private val shifts: ShiftDao,
    private val shiftService: ShiftService,
) {
    suspend fun pastikanAdaSesi() {
        val kasirId = pastikanKasir()
        pastikanShiftTerbuka(kasirId)
    }

    private suspend fun pastikanKasir(): Long =
        cashiers.observeAktif().first().firstOrNull()?.id
            ?: cashiers.insert(
                KasirEntity(
                    id = 0,
                    nama = "Kasir Dev",
                    pinHash = null,   // belum dipakai; diisi saat fitur login PIN ada
                    aktif = true,
                    dibuatPada = System.currentTimeMillis(),
                )
            )

    private suspend fun pastikanShiftTerbuka(kasirId: Long) {
        if (shifts.getOpenForKasir(kasirId) != null) return   // restart dgn shift terbuka -> aman
        shiftService.open(
            cashierId = kasirId,
            openingCash = KAS_AWAL_DEV,
            now = System.currentTimeMillis(),
            note = "Auto-open (dev bootstrap)",
        ).getOrThrow()
    }

    private companion object {
        const val KAS_AWAL_DEV = 0L
    }
}