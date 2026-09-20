package com.sentral.org.data.security

import com.sentral.org.shared.currentTimeMillis
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

sealed interface PinCheckResult {
    data object Success : PinCheckResult
    data class Failed(val sisaPercobaan: Int) : PinCheckResult
    data class Locked(val sisaDetikTerkunci: Long) : PinCheckResult
}

class PinRateLimiter : SynchronizedObject() {
    private val failedAttempts = mutableMapOf<Long, Int>()
    private val lockoutUntil = mutableMapOf<Long, Long>()

    companion object {
        const val MAX_FAILED_ATTEMPTS = 5
        const val LOCKOUT_DURATION_MS = 30_000L // 30 detik penguncian sementara
    }

    /** Memeriksa apakah kasir sedang dalam masa terkunci */
    fun cekStatus(kasirId: Long): Long? = synchronized(this) {
        val now = currentTimeMillis()
        val lockedUntil = lockoutUntil[kasirId] ?: return null
        if (now < lockedUntil) {
            (lockedUntil - now) / 1000L
        } else {
            lockoutUntil.remove(kasirId)
            failedAttempts.remove(kasirId)
            null
        }
    }

    /** Catat kegagalan input PIN */
    fun catatGagal(kasirId: Long): PinCheckResult = synchronized(this) {
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
    fun catatSukses(kasirId: Long) = synchronized(this) {
        failedAttempts.remove(kasirId)
        lockoutUntil.remove(kasirId)
    }
}