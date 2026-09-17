package com.sentral.org.data.repository

import com.sentral.org.data.entity.ItemTransaksiEntity
import com.sentral.org.data.entity.PembayaranEntity
import com.sentral.org.data.entity.TransaksiEntity
import kotlinx.coroutines.flow.Flow

interface TransaksiRepository {
    fun observeAll(): Flow<List<TransaksiEntity>>
    suspend fun getById(id: Long): TransaksiEntity?
    suspend fun getItems(id: Long): List<ItemTransaksiEntity>
    suspend fun getPayments(id: Long): List<PembayaranEntity>
}
