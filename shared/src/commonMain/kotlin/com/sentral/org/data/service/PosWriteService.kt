package com.sentral.org.data.service

import androidx.room3.withWriteTransaction
import com.sentral.org.data.PosDatabase
import com.sentral.org.data.concurrency.PosExecutionLock
import com.sentral.org.data.concurrency.PosExecutionLockImpl

interface PosWriteService {
    suspend fun <T> run(block: suspend () -> T): T
}

class RoomTransactionRunner(
    private val database: PosDatabase,
    private val lock: PosExecutionLock = PosExecutionLockImpl(),
) : PosWriteService {
    override suspend fun <T> run(block: suspend () -> T): T =
        lock.withMutationLock {
            database.withWriteTransaction {
                block()
            }
        }
}
