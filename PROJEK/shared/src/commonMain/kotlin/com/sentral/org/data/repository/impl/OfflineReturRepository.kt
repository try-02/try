package com.sentral.org.data.repository.impl

import com.sentral.org.data.dao.ReturDao
import com.sentral.org.data.repository.ReturRepository

class OfflineReturRepository(private val dao: ReturDao) : ReturRepository {
    override suspend fun getByTransaction(transactionId: Long) = dao.getByTransaction(transactionId)
    override suspend fun getItems(returnId: Long) = dao.getItemsByReturn(returnId)
}
