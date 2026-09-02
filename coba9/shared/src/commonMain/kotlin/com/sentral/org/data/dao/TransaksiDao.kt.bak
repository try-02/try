package com.sentral.org.data.dao

import androidx.paging.PagingSource
import androidx.room3.Dao
import androidx.room3.DaoReturnTypeConverters
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.paging.PagingSourceDaoReturnTypeConverter
import com.sentral.org.data.entity.TransaksiDenganDetail
import com.sentral.org.data.entity.TransaksiEntity
import kotlinx.coroutines.flow.Flow

@Dao
@DaoReturnTypeConverters(PagingSourceDaoReturnTypeConverter::class)
interface TransaksiDao {

    @Insert
    suspend fun insert(entity: TransaksiEntity): Long

    @Query("""
        SELECT * FROM transaksi
        WHERE id = :id
        LIMIT 1
    """)
    suspend fun getById(id: Long): TransaksiEntity?

    @Query("""
        SELECT * FROM transaksi
        ORDER BY dibuat_pada DESC, id DESC
    """)
    fun observeAll(): Flow<List<TransaksiEntity>>

    @Transaction
    @Query("SELECT * FROM transaksi WHERE id = :id LIMIT 1")
    suspend fun getTransaksiUtuhById(id: Long): TransaksiDenganDetail?

    @Query("""
        SELECT * FROM transaksi
        WHERE nomor_transaksi = :number
        LIMIT 1
    """)
    suspend fun getByNumber(number: String): TransaksiEntity?

    @Query("""
        SELECT * FROM transaksi
        ORDER BY dibuat_pada DESC, id DESC
    """)
    fun observeAllPaged(): PagingSource<Int, TransaksiEntity>

    @Query("""
        UPDATE transaksi
        SET status = 'VOID',
            dibatalkan_pada = :now,
            alasan_pembatalan = :reason
        WHERE id = :id
          AND status = 'SELESAI'
    """)
    suspend fun markVoid(
        id: Long,
        now: Long,
        reason: String
    ): Int

    @Query("""
        SELECT EXISTS(
            SELECT 1
            FROM pengembalian
            WHERE transaksi_id = :transactionId
        )
    """)
    suspend fun hasReturns(transactionId: Long): Boolean
}