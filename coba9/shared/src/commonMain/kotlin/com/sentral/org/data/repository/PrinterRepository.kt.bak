package com.sentral.org.data.repository

import com.sentral.org.data.entity.PrinterEntity
import kotlinx.coroutines.flow.Flow

interface PrinterRepository {
    fun observeAll(): Flow<List<PrinterEntity>>
    suspend fun getDefault(): PrinterEntity?
}
