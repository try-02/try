package com.sentral.org.data.dao

import androidx.room3.*
import com.sentral.org.data.entity.KasirEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KasirDao {
    @Insert suspend fun insert(entity: KasirEntity): Long
    @Query("SELECT * FROM kasir WHERE id = :id LIMIT 1") suspend fun getById(id: Long): KasirEntity?
    @Query("SELECT * FROM kasir WHERE aktif = 1 ORDER BY nama COLLATE NOCASE") fun observeAktif(): Flow<List<KasirEntity>>
    @Query("UPDATE kasir SET aktif = :aktif WHERE id = :id") suspend fun setAktif(id: Long, aktif: Boolean): Int
}
