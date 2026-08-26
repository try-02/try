package com.sentral.org.data.dao

import androidx.room3.*
import com.sentral.org.data.entity.ProdukEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProdukDao {
    @Insert suspend fun insert(entity: ProdukEntity): Long
    @Query("SELECT * FROM produk WHERE id = :id LIMIT 1") suspend fun getById(id: Long): ProdukEntity?
    @Query("SELECT * FROM produk WHERE aktif = 1 ORDER BY nama COLLATE NOCASE") fun observeAktif(): Flow<List<ProdukEntity>>
    @Query("SELECT * FROM produk WHERE sku = :sku LIMIT 1") suspend fun getBySku(sku: String): ProdukEntity?
    @Query("SELECT * FROM produk WHERE barcode = :barcode LIMIT 1") suspend fun getByBarcode(barcode: String): ProdukEntity?
    @Query("UPDATE produk SET nama = :nama, harga = :harga, harga_modal = :hargaModal, kategori = :kategori, diperbarui_pada = :waktu WHERE id = :id") suspend fun updateMaster(id: Long, nama: String, harga: Long, hargaModal: Long, kategori: String, waktu: Long): Int
    @Query("UPDATE produk SET aktif = :aktif, diperbarui_pada = :waktu WHERE id = :id") suspend fun setAktif(id: Long, aktif: Boolean, waktu: Long): Int
    @Query("SELECT COUNT(*) FROM produk") suspend fun count(): Int
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertAll(entities: List<ProdukEntity>): List<Long>
}
