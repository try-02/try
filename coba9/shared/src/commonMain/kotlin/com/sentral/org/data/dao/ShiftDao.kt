package com.sentral.org.data.dao

import androidx.room3.*
import com.sentral.org.data.entity.ShiftEntity
import com.sentral.org.data.model.StatusShift

@Dao
interface ShiftDao {
    @Insert suspend fun insert(entity: ShiftEntity): Long
    @Query("SELECT * FROM shift WHERE id = :id LIMIT 1") suspend fun getById(id: Long): ShiftEntity?
    @Query("SELECT * FROM shift WHERE kasir_id = :kasirId AND status = 'TERBUKA' ORDER BY id DESC LIMIT 1") suspend fun getOpenForKasir(kasirId: Long): ShiftEntity?
    @Query("UPDATE shift SET status = 'DITUTUP', kas_diharapkan = :expected, kas_aktual = :actual, selisih_kas = :difference, ditutup_pada = :waktu, catatan = :catatan WHERE id = :id AND status = 'TERBUKA'") suspend fun close(id: Long, expected: Long, actual: Long, difference: Long, waktu: Long, catatan: String): Int
    @Query("SELECT EXISTS(SELECT 1 FROM shift WHERE kasir_id = :kasirId AND status = 'TERBUKA')") suspend fun hasOpenForKasir(kasirId: Long): Boolean
    @Query("SELECT * FROM shift WHERE status = 'TERBUKA' ORDER BY id DESC LIMIT 1")
    suspend fun getLatestOpen(): ShiftEntity?
}
