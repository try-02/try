package com.sentral.org.data.repository.impl

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.sentral.org.data.dao.ItemTransaksiDao
import com.sentral.org.data.dao.PembayaranDao
import com.sentral.org.data.dao.TransaksiDao
import com.sentral.org.data.entity.ItemTransaksiEntity
import com.sentral.org.data.entity.PembayaranEntity
import com.sentral.org.data.entity.TransaksiDenganDetail
import com.sentral.org.data.entity.TransaksiEntity
import com.sentral.org.data.model.TransaksiFilter
import com.sentral.org.data.repository.TransaksiRepository
import kotlinx.coroutines.flow.Flow

class OfflineTransaksiRepository(
    private val transactions: TransaksiDao,
    private val items: ItemTransaksiDao,
    private val payments: PembayaranDao,
) : TransaksiRepository {

    override fun observeAll(): Flow<List<TransaksiEntity>> = transactions.observeAll()

    override fun getRiwayatPaged(filter: TransaksiFilter): Flow<PagingData<TransaksiEntity>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 5,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = {
                transactions.observeFilteredPaged(
                    query = filter.query.trim(),
                    status = filter.status,
                    startDate = filter.startDate,
                    endDate = filter.endDate,
                )
            },
        ).flow
    }

    override suspend fun getById(id: Long): TransaksiEntity? = transactions.getById(id)

    override suspend fun getDetailById(id: Long): TransaksiDenganDetail? = transactions.getTransaksiUtuhById(id)

    override suspend fun getItems(id: Long): List<ItemTransaksiEntity> = items.getByTransaction(id)

    override suspend fun getPayments(id: Long): List<PembayaranEntity> = payments.getByTransaction(id)

    override suspend fun getTransaksiForExportPaged(
        filter: TransaksiFilter,
        limit: Int,
        offset: Int,
    ): List<TransaksiDenganDetail> {
        return transactions.getFilteredForExportPaged(
            query = filter.query.trim(),
            status = filter.status,
            startDate = filter.startDate,
            endDate = filter.endDate,
            limit = limit,
            offset = offset,
        )
    }
}
