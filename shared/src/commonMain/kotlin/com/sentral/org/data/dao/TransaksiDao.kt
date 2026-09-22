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
import com.sentral.org.data.model.MetodePembayaran
import com.sentral.org.data.model.StatusTransaksi
import kotlinx.coroutines.flow.Flow

data class SalesAggregateRaw(
    val omzetPenjualan: Long,
    val jumlahSelesai: Int,
    val jumlahVoid: Int,
)

data class PaymentAggregateRaw(
    val metode: MetodePembayaran,
    val totalNominal: Long,
)

data class TopProductRaw(
    val produkId: Long,
    val namaProduk: String,
    val totalJumlahScaled: Long,
    val totalNominal: Long,
)

@Dao
@DaoReturnTypeConverters(PagingSourceDaoReturnTypeConverter::class)
interface TransaksiDao {

    @Insert
    suspend fun insert(entity: TransaksiEntity): Long

    @Query("SELECT * FROM transaksi WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TransaksiEntity?

    @Query("SELECT * FROM transaksi ORDER BY dibuat_pada DESC, id DESC")
    fun observeAll(): Flow<List<TransaksiEntity>>

    @Transaction
    @Query("SELECT * FROM transaksi WHERE id = :id LIMIT 1")
    suspend fun getTransaksiUtuhById(id: Long): TransaksiDenganDetail?

    @Query("SELECT * FROM transaksi WHERE nomor_transaksi = :number LIMIT 1")
    suspend fun getByNumber(number: String): TransaksiEntity?

    @Query("SELECT * FROM transaksi ORDER BY dibuat_pada DESC, id DESC")
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

    // ===== QUERY ANALITIK REAKTIF TAHAP 7 =====

    @Query("""
        SELECT 
            COALESCE(SUM(CASE WHEN status = 'SELESAI' THEN total ELSE 0 END), 0) AS omzetPenjualan,
            COALESCE(SUM(CASE WHEN status = 'SELESAI' THEN 1 ELSE 0 END), 0) AS jumlahSelesai,
            COALESCE(SUM(CASE WHEN status = 'VOID' THEN 1 ELSE 0 END), 0) AS jumlahVoid
        FROM transaksi
        WHERE dibuat_pada >= :start AND dibuat_pada < :end
    """)
    fun observeSalesAggregate(start: Long, end: Long): Flow<SalesAggregateRaw>

    @Query("""
        SELECT COALESCE(SUM((it.harga_modal * it.jumlah + 500) / 1000), 0)
        FROM item_transaksi it
        JOIN transaksi t ON it.transaksi_id = t.id
        WHERE t.status = 'SELESAI' AND t.dibuat_pada >= :start AND t.dibuat_pada < :end
    """)
    fun observeHppPenjualan(start: Long, end: Long): Flow<Long>

    @Query("""
        SELECT 
            p.metode AS metode,
            COALESCE(SUM(p.jumlah), 0) AS totalNominal
        FROM pembayaran p
        JOIN transaksi t ON p.transaksi_id = t.id
        WHERE t.status = 'SELESAI' AND t.dibuat_pada >= :start AND t.dibuat_pada < :end
        GROUP BY p.metode
    """)
    fun observePaymentAggregate(start: Long, end: Long): Flow<List<PaymentAggregateRaw>>

    @Query("""
        SELECT 
            COALESCE(it.produk_id, 0) AS produkId,
            it.nama_produk AS namaProduk,
            COALESCE(SUM(it.jumlah), 0) AS totalJumlahScaled,
            COALESCE(SUM(it.total_baris - it.diskon_item), 0) AS totalNominal
        FROM item_transaksi it
        JOIN transaksi t ON it.transaksi_id = t.id
        WHERE t.status = 'SELESAI' AND t.dibuat_pada >= :start AND t.dibuat_pada < :end
        GROUP BY it.nama_produk
        ORDER BY totalJumlahScaled DESC
        LIMIT 5
    """)
    fun observeTopProducts(start: Long, end: Long): Flow<List<TopProductRaw>>
}