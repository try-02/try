package com.sentral.org.data.security

import com.sentral.org.shared.currentTimeMillis
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface PinCheckResult {
    data object Success : PinCheckResult

    data class Failed(
        val sisaPercobaan: Int,
    ) : PinCheckResult

    data class Locked(
        val sisaDetikTerkunci: Long,
    ) : PinCheckResult
}

class PinRateLimiter {
    private val failedAttempts = mutableMapOf<Long, Int>()
    private val lockoutUntil = mutableMapOf<Long, Long>()
    private val mutex = Mutex()

    companion object {
        const val MAX_FAILED_ATTEMPTS = 5
        const val LOCKOUT_DURATION_MS = 30_000L // 30 detik penguncian sementara
    }

    /** Memeriksa apakah kasir sedang dalam masa terkunci */
    suspend fun cekStatus(kasirId: Long): Long? =
        mutex.withLock {
            val now = currentTimeMillis()
            val lockedUntil = lockoutUntil[kasirId] ?: return@withLock null
            if (now < lockedUntil) {
                (lockedUntil - now) / 1000L
            } else {
                lockoutUntil.remove(kasirId)
                failedAttempts.remove(kasirId)
                null
            }
        }

    /** Catat kegagalan input PIN */
    suspend fun catatGagal(kasirId: Long): PinCheckResult =
        mutex.withLock {
            val now = currentTimeMillis()
            val current = (failedAttempts[kasirId] ?: 0) + 1
            failedAttempts[kasirId] = current

            if (current >= MAX_FAILED_ATTEMPTS) {
                val lockTime = now + LOCKOUT_DURATION_MS
                lockoutUntil[kasirId] = lockTime
                PinCheckResult.Locked(LOCKOUT_DURATION_MS / 1000L)
            } else {
                PinCheckResult.Failed(MAX_FAILED_ATTEMPTS - current)
            }
        }

    /** Reset catatan saat PIN berhasil */
    suspend fun catatSukses(kasirId: Long): Unit =
        mutex.withLock {
            failedAttempts.remove(kasirId)
            lockoutUntil.remove(kasirId)
        }
}
