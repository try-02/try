package com.sentral.org.data.repository.impl

import com.sentral.org.data.dao.PersediaanDao
import com.sentral.org.data.dao.PergerakanPersediaanDao
import com.sentral.org.data.repository.PersediaanRepository

class OfflinePersediaanRepository(private val stock: PersediaanDao, private val ledger: PergerakanPersediaanDao) : PersediaanRepository {
    override suspend fun getByProduct(productId: Long) = stock.getByProdukId(productId)
    override suspend fun getHistory(productId: Long) = ledger.getByProduk(productId)
}
