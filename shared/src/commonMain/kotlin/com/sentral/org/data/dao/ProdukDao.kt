package com.sentral.org.data.dao

import androidx.room3.*
import com.sentral.org.data.entity.ProdukEntity
import kotlinx.coroutines.flow.Flow

data class ProdukDenganStokRaw(
    @Embedded val produk: ProdukEntity,
    @ColumnInfo(name = "stok_normal") val stokNormal: Long?,
    @ColumnInfo(name = "stok_rusak") val stokRusak: Long?,
)

@Dao
interface ProdukDao {
    @Insert suspend fun insert(entity: ProdukEntity): Long

    @Query("SELECT * FROM produk WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ProdukEntity?

    @Query("SELECT * FROM produk WHERE aktif = 1 ORDER BY nama COLLATE NOCASE")
    fun observeAktif(): Flow<List<ProdukEntity>>

    @Query("SELECT * FROM produk WHERE sku = :sku LIMIT 1")
    suspend fun getBySku(sku: String): ProdukEntity?

    @Query("SELECT * FROM produk WHERE barcode = :barcode LIMIT 1")
    suspend fun getByBarcode(barcode: String): ProdukEntity?

    @Query(
        "UPDATE produk SET nama = :nama, sku = :sku, barcode = :barcode, harga = :harga, harga_modal = :hargaModal, kategori = :kategori, diperbarui_pada = :waktu WHERE id = :id",
    )
    suspend fun updateMaster(
        id: Long,
        nama: String,
        sku: String,
        barcode: String?,
        harga: Long,
        hargaModal: Long,
        kategori: String,
        waktu: Long,
    ): Int

    @Query("UPDATE produk SET aktif = :aktif, diperbarui_pada = :waktu WHERE id = :id")
    suspend fun setAktif(
        id: Long,
        aktif: Boolean,
        waktu: Long,
    ): Int

    @Query("SELECT COUNT(*) FROM produk")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entities: List<ProdukEntity>): List<Long>

    // ===== QUERY INVENTARIS & KATALOG ADMIN =====

    @Query(
        """
        SELECT p.*, s.jumlah AS stok_normal, s.jumlah_rusak AS stok_rusak
        FROM produk p
        LEFT JOIN persediaan s ON p.id = s.produk_id
        WHERE (:hanyaAktif = 0 OR p.aktif = 1)
          AND (:kategori IS NULL OR p.kategori = :kategori)
          AND (:query = '' OR p.nama LIKE '%' || :query || '%' OR p.sku LIKE '%' || :query || '%' OR p.barcode LIKE '%' || :query || '%')
        ORDER BY p.nama COLLATE NOCASE ASC
    """,
    )
    fun observeProdukAdmin(
        query: String = "",
        kategori: String? = null,
        hanyaAktif: Boolean = false,
    ): Flow<List<ProdukDenganStokRaw>>

    @Query("SELECT EXISTS(SELECT 1 FROM produk WHERE sku = :sku AND (:excludeId IS NULL OR id != :excludeId))")
    suspend fun existsBySku(
        sku: String,
        excludeId: Long? = null,
    ): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM produk WHERE barcode = :barcode AND (:excludeId IS NULL OR id != :excludeId))")
    suspend fun existsByBarcode(
        barcode: String,
        excludeId: Long? = null,
    ): Boolean

    @Query("SELECT DISTINCT kategori FROM produk WHERE kategori != '' ORDER BY kategori COLLATE NOCASE ASC")
    fun observeSemuaKategori(): Flow<List<String>>
}
