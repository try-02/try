package com.sentral.org.data.session

import com.sentral.org.data.dao.KasirDao
import com.sentral.org.data.dao.ShiftDao
import com.sentral.org.data.model.StatusShift
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ActiveSesiKasirProvider(
    private val kasirDao: KasirDao,
    private val shiftDao: ShiftDao,
) : SesiKasirProvider {
    // Runtime pointer kasir yang sedang login
    private val _kasirLoginId = MutableStateFlow<Long?>(null)
    val kasirLoginId: StateFlow<Long?> = _kasirLoginId.asStateFlow()

    private val mutex = Mutex()

    /**
     * Sumber Kebenaran Mutlak: Selalu periksa DB SQLite apakah shift kasir masih TERBUKA.
     */
    override suspend fun sesiAktif(): SesiKasir? =
        mutex.withLock {
            val activeKasirId = _kasirLoginId.value

            if (activeKasirId != null) {
                // Skenario normal: Kasir sedang login di memori
                val shift = shiftDao.getOpenForKasir(activeKasirId)
                if (shift != null && shift.status == StatusShift.TERBUKA) {
                    return SesiKasir(
                        kasirId = shift.kasirId,
                        namaKasir = shift.namaKasir,
                        shiftId = shift.id,
                    )
                }
            } else {
                // Skenario Pemulihan (App Restart / Process Death):
                // Cek apakah ada shift yang masih menggantung TERBUKA di DB
                val latestOpen = shiftDao.getLatestOpen()
                if (latestOpen != null && latestOpen.status == StatusShift.TERBUKA) {
                    _kasirLoginId.value = latestOpen.kasirId
                    return SesiKasir(
                        kasirId = latestOpen.kasirId,
                        namaKasir = latestOpen.namaKasir,
                        shiftId = latestOpen.id,
                    )
                }
            }

            return null
        }

    /** Login kasir setelah verifikasi PIN berhasil */
    fun setKasirLogin(kasirId: Long) {
        _kasirLoginId.value = kasirId
    }

    /** Logout kasir / kunci terminal */
    fun logout() {
        _kasirLoginId.value = null
    }
}
