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
import com.sentral.org.data.model.StatusTransaksi
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
        SELECT * FROM transaksi
        WHERE (:query = '' OR nomor_transaksi LIKE :query || '%')
          AND (:status IS NULL OR status = :status)
          AND (:startDate IS NULL OR dibuat_pada >= :startDate)
          AND (:endDate IS NULL OR dibuat_pada <= :endDate)
        ORDER BY dibuat_pada DESC, id DESC
    """)
    fun observeFilteredPaged(
        query: String,
        status: StatusTransaksi?,
        startDate: Long?,
        endDate: Long?,
    ): PagingSource<Int, TransaksiEntity>

    @Transaction
    @Query("""
        SELECT * FROM transaksi
        WHERE (:query = '' OR nomor_transaksi LIKE :query || '%')
          AND (:status IS NULL OR status = :status)
          AND (:startDate IS NULL OR dibuat_pada >= :startDate)
          AND (:endDate IS NULL OR dibuat_pada <= :endDate)
        ORDER BY dibuat_pada DESC, id DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getFilteredForExportPaged(
        query: String,
        status: StatusTransaksi?,
        startDate: Long?,
        endDate: Long?,
        limit: Int,
        offset: Int,
    ): List<TransaksiDenganDetail>

    @Query("""
        SELECT * FROM transaksi
        WHERE shift_id = :shiftId
        ORDER BY dibuat_pada ASC
    """)
    suspend fun getByShift(shiftId: Long): List<TransaksiEntity>
}