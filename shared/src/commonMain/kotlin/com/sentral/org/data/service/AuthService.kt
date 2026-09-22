package com.sentral.org.data.service

import com.sentral.org.data.dao.KasirDao
import com.sentral.org.data.dao.ShiftDao
import com.sentral.org.data.model.StatusShift
import com.sentral.org.data.security.PinCheckResult
import com.sentral.org.data.security.PinHasher
import com.sentral.org.data.security.PinRateLimiter
import com.sentral.org.data.session.ActiveSesiKasirProvider

sealed interface AuthResult {
    data class Success(
        val kasirId: Long,
        val namaKasir: String,
        val hasOpenShift: Boolean,
    ) : AuthResult

    data class Failed(
        val sisaPercobaan: Int,
    ) : AuthResult

    data class Locked(
        val sisaDetik: Long,
    ) : AuthResult

    data object KasirTidakAktif : AuthResult
}

class AuthService(
    private val kasirDao: KasirDao,
    private val shiftDao: ShiftDao,
    private val pinHasher: PinHasher,
    private val rateLimiter: PinRateLimiter,
    private val sessionProvider: ActiveSesiKasirProvider,
) {
    /**
     * Memverifikasi PIN kasir dengan KDF PBKDF2, menerapkan rate-limit brute-force,
     * dan mengecek status shift otoritatif langsung dari SQLite.
     */
    suspend fun login(
        kasirId: Long,
        rawPin: String,
    ): AuthResult {
        // 1. Cek apakah kasir sedang terkunci sementara (Rate Limit Lockout)
        val lockedSeconds = rateLimiter.cekStatus(kasirId)
        if (lockedSeconds != null) {
            return AuthResult.Locked(lockedSeconds)
        }

        // 2. Pastikan kasir ada dan aktif
        val kasir = kasirDao.getById(kasirId) ?: return AuthResult.KasirTidakAktif
        if (!kasir.aktif) return AuthResult.KasirTidakAktif

        // 3. Verifikasi hash PIN (fallback 123456 jika data seed awal belum memiliki hash)
        val storedHash = kasir.pinHash
        val isPinValid =
            if (storedHash.isNullOrBlank()) {
                rawPin == "123456"
            } else {
                pinHasher.verifyPin(rawPin, storedHash)
            }

        if (!isPinValid) {
            return when (val check = rateLimiter.catatGagal(kasirId)) {
                is PinCheckResult.Failed -> AuthResult.Failed(check.sisaPercobaan)
                is PinCheckResult.Locked -> AuthResult.Locked(check.sisaDetikTerkunci)
                PinCheckResult.Success -> AuthResult.Failed(0)
            }
        }

        // 4. Sukses: Reset rate limiter dan set pointer kasir di sesi
        rateLimiter.catatSukses(kasirId)
        sessionProvider.setKasirLogin(kasirId)

        // 5. Periksa sumber kebenaran (Authoritative DB): Apakah kasir ini sudah punya shift aktif?
        val openShift = shiftDao.getOpenForKasir(kasirId)
        val hasOpenShift = openShift != null && openShift.status == StatusShift.TERBUKA

        return AuthResult.Success(
            kasirId = kasir.id,
            namaKasir = kasir.nama,
            hasOpenShift = hasOpenShift,
        )
    }

    /** Menetapkan atau memperbarui PIN kasir dengan hash KDF PBKDF2 baru */
    fun buatHashPin(rawPin: String): String {
        require(rawPin.length in 4..6) { "PIN harus terdiri dari 4-6 angka" }
        return pinHasher.hashPin(rawPin)
    }
}
