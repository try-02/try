package com.sentral.org.data.repository.impl

import com.sentral.org.data.dao.ShiftDao
import com.sentral.org.data.repository.ShiftRepository

class OfflineShiftRepository(private val dao: ShiftDao) : ShiftRepository {
    override suspend fun getById(id: Long) = dao.getById(id)
    override suspend fun getOpenForKasir(kasirId: Long) = dao.getOpenForKasir(kasirId)
}
