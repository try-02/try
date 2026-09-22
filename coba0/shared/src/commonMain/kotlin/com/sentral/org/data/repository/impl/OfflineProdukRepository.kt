package com.sentral.org.data.repository.impl

import com.sentral.org.data.dao.ProdukDao
import com.sentral.org.data.entity.ProdukEntity
import com.sentral.org.data.repository.ProdukRepository
import kotlinx.coroutines.flow.Flow

class OfflineProdukRepository(
    private val dao: ProdukDao,
) : ProdukRepository {
    override fun observeAktif(): Flow<List<ProdukEntity>> = dao.observeAktif()

    override suspend fun getById(id: Long): ProdukEntity? = dao.getById(id)

    override suspend fun getBySku(sku: String): ProdukEntity? = dao.getBySku(sku)

    override suspend fun getByBarcode(barcode: String): ProdukEntity? = dao.getByBarcode(barcode)

    override suspend fun insert(entity: ProdukEntity): Long = dao.insert(entity)

    override suspend fun updateMaster(
        id: Long,
        nama: String,
        sku: String,
        barcode: String?,
        harga: Long,
        hargaModal: Long,
        kategori: String,
        waktu: Long,
    ): Int = dao.updateMaster(id, nama, sku, barcode, harga, hargaModal, kategori, waktu)

    override suspend fun setAktif(
        id: Long,
        aktif: Boolean,
        waktu: Long,
    ): Int = dao.setAktif(id, aktif, waktu)
}
