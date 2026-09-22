package com.sentral.org.data.repository

import com.sentral.org.data.entity.KasirEntity
import kotlinx.coroutines.flow.Flow

interface KasirRepository {
    fun observeAktif(): Flow<List<KasirEntity>>
    suspend fun getById(id: Long): KasirEntity?
}
