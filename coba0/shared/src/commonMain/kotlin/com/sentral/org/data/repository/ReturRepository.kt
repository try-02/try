package com.sentral.org.data.repository

import com.sentral.org.data.entity.ItemPengembalianEntity
import com.sentral.org.data.entity.PengembalianEntity

interface ReturRepository {
    suspend fun getByTransaction(transactionId: Long): List<PengembalianEntity>
    suspend fun getItems(returnId: Long): List<ItemPengembalianEntity>
}
