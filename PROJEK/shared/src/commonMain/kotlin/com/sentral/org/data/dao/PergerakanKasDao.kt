package com.sentral.org.data.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import com.sentral.org.data.entity.PergerakanKasEntity

@Dao
interface PergerakanKasDao {
    @Insert
    suspend fun insert(entity: PergerakanKasEntity): Long
    @Query("""
        SELECT * FROM pergerakan_kas
        WHERE shift_id = :shiftId
        ORDER BY dibuat_pada, id
    """)
    suspend fun getByShift(shiftId: Long): List<PergerakanKasEntity>
    @Query("""
        SELECT COALESCE(SUM(jumlah_delta),0) FROM pergerakan_kas
        WHERE shift_id = :shiftId
    """)
    suspend fun getExpectedCash(shiftId: Long): Long
}
