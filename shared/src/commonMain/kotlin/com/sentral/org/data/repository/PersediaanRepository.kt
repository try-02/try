package com.sentral.org.data.repository

import com.sentral.org.data.entity.PergerakanPersediaanEntity
import com.sentral.org.data.entity.PersediaanEntity

interface PersediaanRepository {
    suspend fun getByProduct(productId: Long): PersediaanEntity?

    suspend fun getHistory(productId: Long): List<PergerakanPersediaanEntity>
}
