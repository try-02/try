package com.sentral.org.data.dao

import androidx.room3.*
import com.sentral.org.data.entity.PersediaanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersediaanDao {
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insert(entity: PersediaanEntity)
    @Query("SELECT * FROM persediaan WHERE produk_id = :produkId LIMIT 1") suspend fun getByProdukId(produkId: Long): PersediaanEntity?
    @Query("UPDATE persediaan SET jumlah = jumlah + :delta, diperbarui_pada = :waktu WHERE produk_id = :produkId") suspend fun addNormal(produkId: Long, delta: Long, waktu: Long): Int
    @Query("UPDATE persediaan SET jumlah_rusak = jumlah_rusak + :delta, diperbarui_pada = :waktu WHERE produk_id = :produkId AND jumlah_rusak + :delta >= 0") suspend fun addDamaged(produkId: Long, delta: Long, waktu: Long): Int
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertAll(entities: List<PersediaanEntity>)
    @Query("SELECT * FROM persediaan") fun observeAll(): Flow<List<PersediaanEntity>>
}
