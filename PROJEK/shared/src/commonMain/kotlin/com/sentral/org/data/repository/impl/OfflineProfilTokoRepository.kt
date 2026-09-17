package com.sentral.org.data.repository.impl

import com.sentral.org.data.dao.ProfilTokoDao
import com.sentral.org.data.repository.ProfilTokoRepository

class OfflineProfilTokoRepository(private val dao: ProfilTokoDao) : ProfilTokoRepository {
    override fun observe() = dao.observe()
    override suspend fun get() = dao.get()
    override suspend fun save(entity: com.sentral.org.data.entity.ProfilTokoEntity) = dao.save(entity)
}
