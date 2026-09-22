package com.sentral.org.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import com.sentral.org.data.entity.ItemPengembalianEntity
import com.sentral.org.data.entity.PengembalianEntity
import kotlinx.coroutines.flow.Flow

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

    // ===== QUERY ANALITIK REAKTIF TAHAP 7 =====

    @Query("""
        SELECT COALESCE(SUM(jumlah_pengembalian), 0)
        FROM pengembalian
        WHERE dikembalikan_pada >= :start AND dikembalikan_pada < :end
    """)
    fun observeTotalRefund(start: Long, end: Long): Flow<Long>

    @Query("""
        SELECT COALESCE(SUM((it.harga_modal * ip.jumlah_dikembalikan + 500) / 1000), 0)
        FROM item_pengembalian ip
        JOIN item_transaksi it ON ip.item_transaksi_id = it.id
        JOIN pengembalian p ON ip.pengembalian_id = p.id
        WHERE p.dikembalikan_pada >= :start AND p.dikembalikan_pada < :end
    """)
    fun observeHppRetur(start: Long, end: Long): Flow<Long>
}