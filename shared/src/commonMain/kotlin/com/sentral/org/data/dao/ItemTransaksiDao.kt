package com.sentral.org.data.dao

import androidx.room3.*
import com.sentral.org.data.entity.ItemTransaksiEntity

@Dao
interface ItemTransaksiDao {
    @Insert suspend fun insertAll(items: List<ItemTransaksiEntity>): List<Long>
    @Query("SELECT * FROM item_transaksi WHERE transaksi_id=:transactionId ORDER BY id") suspend fun getByTransaction(transactionId: Long): List<ItemTransaksiEntity>
    @Query("SELECT * FROM item_transaksi WHERE id=:id LIMIT 1") suspend fun getById(id: Long): ItemTransaksiEntity?
}
