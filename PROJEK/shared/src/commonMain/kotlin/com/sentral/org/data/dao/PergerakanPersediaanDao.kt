package com.sentral.org.data.dao

import androidx.room3.*
import com.sentral.org.data.entity.PergerakanPersediaanEntity

@Dao
interface PergerakanPersediaanDao {
    @Insert
    suspend fun insert(entity: PergerakanPersediaanEntity): Long
    @Query("""
        SELECT * FROM pergerakan_persediaan
        WHERE produk_id = :produkId
        ORDER BY dibuat_pada DESC, id DESC
    """)
    suspend fun getByProduk(produkId: Long): List<PergerakanPersediaanEntity>
    @Query("""
        SELECT * FROM pergerakan_persediaan
        WHERE transaksi_id = :transaksiId
        ORDER BY id
    """)
    suspend fun getByTransaksi(transaksiId: Long): List<PergerakanPersediaanEntity>
    @Query("""
        SELECT * FROM pergerakan_persediaan
        WHERE pengembalian_id = :returId
        ORDER BY id
    """)
    suspend fun getByRetur(returId: Long): List<PergerakanPersediaanEntity>
    @Insert
    suspend fun insertAll(entities: List<PergerakanPersediaanEntity>)
}
