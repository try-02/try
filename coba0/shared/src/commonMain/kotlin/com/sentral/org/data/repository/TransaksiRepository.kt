package com.sentral.org.data.repository

import androidx.paging.PagingData
import com.sentral.org.data.entity.ItemTransaksiEntity
import com.sentral.org.data.entity.PembayaranEntity
import com.sentral.org.data.entity.TransaksiDenganDetail
import com.sentral.org.data.entity.TransaksiEntity
import com.sentral.org.data.model.TransaksiFilter
import kotlinx.coroutines.flow.Flow

interface TransaksiRepository {
    fun observeAll(): Flow<List<TransaksiEntity>>

    fun getRiwayatPaged(filter: TransaksiFilter): Flow<PagingData<TransaksiEntity>>

    suspend fun getById(id: Long): TransaksiEntity?

    suspend fun getDetailById(id: Long): TransaksiDenganDetail?

    suspend fun getItems(id: Long): List<ItemTransaksiEntity>

    suspend fun getPayments(id: Long): List<PembayaranEntity>

    suspend fun getTransaksiForExportPaged(
        filter: TransaksiFilter,
        limit: Int,
        offset: Int,
    ): List<TransaksiDenganDetail>
}
