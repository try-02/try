package com.sentral.org.data.repository

import com.sentral.org.data.entity.ProfilTokoEntity
import kotlinx.coroutines.flow.Flow

interface ProfilTokoRepository {
    fun observe(): Flow<ProfilTokoEntity?>
    suspend fun get(): ProfilTokoEntity?
    suspend fun save(entity: ProfilTokoEntity)
}
