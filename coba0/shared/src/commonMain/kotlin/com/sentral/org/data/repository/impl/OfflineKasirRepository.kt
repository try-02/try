package com.sentral.org.data.repository.impl

import com.sentral.org.data.dao.KasirDao
import com.sentral.org.data.repository.KasirRepository

class OfflineKasirRepository(private val dao: KasirDao) : KasirRepository {
    override fun observeAktif() = dao.observeAktif()
    override suspend fun getById(id: Long) = dao.getById(id)
}
