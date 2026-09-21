package com.sentral.org.data.repository

import com.sentral.org.data.entity.ProdukEntity
import kotlinx.coroutines.flow.Flow

interface ProdukRepository {
    fun observeAktif(): Flow<List<ProdukEntity>>
    suspend fun getById(id: Long): ProdukEntity?
    suspend fun getBySku(sku: String): ProdukEntity?
    suspend fun getByBarcode(barcode: String): ProdukEntity?
    suspend fun insert(entity: ProdukEntity): Long
    suspend fun updateMaster(id: Long, nama: String, harga: Long, hargaModal: Long, kategori: String, waktu: Long): Int
    suspend fun setAktif(id: Long, aktif: Boolean, waktu: Long): Int
}
