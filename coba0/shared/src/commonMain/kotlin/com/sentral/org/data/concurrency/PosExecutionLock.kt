package com.sentral.org.data.concurrency

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class ExecutionMode { IDLE, MUTATION, BACKUP, RESTORE }

interface PosExecutionLock {
    val currentMode: ExecutionMode

    suspend fun <T> withMutationLock(block: suspend () -> T): T

    suspend fun <T> withBackupLock(
        failIfBusy: Boolean = false,
        block: suspend () -> T,
    ): T

    suspend fun <T> withRestoreLock(block: suspend () -> T): T
}

class PosExecutionLockImpl : PosExecutionLock {
    private val mutex = Mutex()
    private var _mode = ExecutionMode.IDLE
    override val currentMode: ExecutionMode get() = _mode

    override suspend fun <T> withMutationLock(block: suspend () -> T): T =
        mutex.withLock {
            _mode = ExecutionMode.MUTATION
            try {
                block()
            } finally {
                _mode = ExecutionMode.IDLE
            }
        }

    override suspend fun <T> withBackupLock(
        failIfBusy: Boolean,
        block: suspend () -> T,
    ): T {
        if (failIfBusy && mutex.isLocked) {
            throw IllegalStateException("Transaksi finansial sedang diproses. Selesaikan transaksi sebelum membuat backup.")
        }
        return mutex.withLock {
            _mode = ExecutionMode.BACKUP
            try {
                block()
            } finally {
                _mode = ExecutionMode.IDLE
            }
        }
    }

    override suspend fun <T> withRestoreLock(block: suspend () -> T): T =
        mutex.withLock {
            _mode = ExecutionMode.RESTORE
            try {
                block()
            } finally {
                _mode = ExecutionMode.IDLE
            }
        }
}
