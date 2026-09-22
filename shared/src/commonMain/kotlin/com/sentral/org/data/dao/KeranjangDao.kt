package com.sentral.org.data.dao

import androidx.room3.*
import com.sentral.org.data.entity.KeranjangEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KeranjangDao {
    @Insert
    suspend fun insert(entity: KeranjangEntity): Long
    @Query("""
        SELECT * FROM keranjang
        WHERE id = :id LIMIT 1
    """)
    suspend fun getById(id: Long): KeranjangEntity?
    @Query("""
        SELECT * FROM keranjang
        WHERE status IN ('AKTIF','DITAHAN')
        ORDER BY diperbarui_pada DESC
    """)
    fun observeOpen(): Flow<List<KeranjangEntity>>
    @Query("""
        UPDATE keranjang SET status = 'DITAHAN', ditahan_pada = :now, diperbarui_pada = :now
        WHERE id = :id AND
        status = 'AKTIF'
    """)
    suspend fun hold(id: Long, now: Long): Int
    @Query("""
        UPDATE keranjang SET status = 'AKTIF', diperbarui_pada = :now 
        WHERE id = :id AND
        status = 'DITAHAN'
    """)
    suspend fun resume(id: Long, now: Long): Int
    @Query("""
        UPDATE keranjang SET status = 'DIBATALKAN', dibatalkan_pada = :now, diperbarui_pada = :now
        WHERE id = :id AND
        status IN ('AKTIF','DITAHAN')
    """)
    suspend fun cancel(id: Long, now: Long): Int
    @Query("""
        UPDATE keranjang SET status = 'SELESAI', diselesaikan_pada = :now, diperbarui_pada = :now
        WHERE id = :id AND status = 'AKTIF'
    """)
    suspend fun complete(id: Long, now: Long): Int
}
