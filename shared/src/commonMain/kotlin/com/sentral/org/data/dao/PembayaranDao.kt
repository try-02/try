package com.sentral.org.data.dao

import androidx.room3.*
import com.sentral.org.data.entity.PembayaranEntity

@Dao
interface PembayaranDao {
    @Insert suspend fun insert(entity: PembayaranEntity): Long
    @Insert suspend fun insertAll(items: List<PembayaranEntity>): List<Long>
    @Query("SELECT * FROM pembayaran WHERE transaksi_id=:transactionId ORDER BY id") suspend fun getByTransaction(transactionId: Long): List<PembayaranEntity>
}
