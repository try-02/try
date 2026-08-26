package com.sentral.org.data.service

import androidx.room3.withWriteTransaction
import com.sentral.org.data.PosDatabase

interface PosWriteService {

    suspend fun <T> run(block: suspend () -> T): T
}

class RoomTransactionRunner(
    private val database: PosDatabase
) : PosWriteService {

    override suspend fun <T> run(block: suspend () -> T): T {
        return database.withWriteTransaction {
            block()
        }
    }
}