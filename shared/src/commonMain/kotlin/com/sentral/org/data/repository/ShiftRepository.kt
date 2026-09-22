package com.sentral.org.data.repository

import com.sentral.org.data.entity.ShiftEntity

interface ShiftRepository {
    suspend fun getById(id: Long): ShiftEntity?
    suspend fun getOpenForKasir(kasirId: Long): ShiftEntity?
}
