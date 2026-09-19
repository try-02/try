package com.sentral.org.data.dao

import androidx.room3.*
import com.sentral.org.data.entity.PrinterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PrinterDao {
    @Insert suspend fun insert(entity: PrinterEntity): Long
    @Update suspend fun update(entity: PrinterEntity)
    @Query("SELECT * FROM printer ORDER BY prioritas, id") fun observeAll(): Flow<List<PrinterEntity>>
    @Query("SELECT * FROM printer WHERE is_default = 1 ORDER BY prioritas, id LIMIT 1") suspend fun getDefault(): PrinterEntity?
    @Query("UPDATE printer SET gagal_status_berturut=:failures, dinonaktifkan_otomatis=:disabled WHERE id=:id") suspend fun updateHealth(id: Long, failures: Int, disabled: Boolean): Int
}
