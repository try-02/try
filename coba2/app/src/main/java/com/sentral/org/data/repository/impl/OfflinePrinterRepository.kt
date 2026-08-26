package com.sentral.org.data.repository.impl

import com.sentral.org.data.dao.PrinterDao
import com.sentral.org.data.repository.PrinterRepository

class OfflinePrinterRepository(private val dao: PrinterDao) : PrinterRepository {
    override fun observeAll() = dao.observeAll()
    override suspend fun getDefault() = dao.getDefault()
}
