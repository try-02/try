package com.sentral.org.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import com.sentral.org.data.entity.ItemPengembalianEntity
import com.sentral.org.data.entity.PengembalianEntity

@Dao
interface ReturDao {

    @Insert
    suspend fun insert(entity: PengembalianEntity): Long

    @Insert
    suspend fun insertItems(
        items: List<ItemPengembalianEntity>
    ): List<Long>

    @Query("""
        SELECT * FROM pengembalian
        WHERE id = :id
        LIMIT 1
    """)
    suspend fun getById(id: Long): PengembalianEntity?

    @Query("""
        SELECT * FROM pengembalian
        WHERE transaksi_id = :transactionId 
        ORDER BY dikembalikan_pada, id
    """)
    suspend fun getByTransaction(transactionId: Long): List<PengembalianEntity>

    @Query("""
        SELECT * FROM item_pengembalian
        WHERE pengembalian_id = :returnId
        ORDER BY id
    """)
    suspend fun getItemsByReturn(returnId: Long): List<ItemPengembalianEntity>

    @Query("""
        SELECT EXISTS(SELECT 1 FROM pengembalian
        WHERE transaksi_id = :transactionId)
    """)

    suspend fun existsForTransaction(transactionId: Long): Boolean
    @Query("""
        SELECT COALESCE(SUM(jumlah_dikembalikan),0) FROM item_pengembalian
        WHERE item_transaksi_id = :itemTransactionId
    """)

    suspend fun getReturnedQuantity(itemTransactionId: Long): Long

    @Query("""
        SELECT COALESCE(SUM(jumlah_refund),0) FROM item_pengembalian
        WHERE item_transaksi_id = :itemTransactionId
    """)
    suspend fun getRefundTotal(itemTransactionId: Long): Long
}