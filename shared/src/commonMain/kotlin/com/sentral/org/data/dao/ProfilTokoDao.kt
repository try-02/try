package com.sentral.org.data.dao

import androidx.room3.*
import com.sentral.org.data.entity.ProfilTokoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfilTokoDao {
    @Query("SELECT * FROM profil_toko WHERE id=1 LIMIT 1") suspend fun get(): ProfilTokoEntity?
    @Query("SELECT * FROM profil_toko WHERE id=1 LIMIT 1") fun observe(): Flow<ProfilTokoEntity?>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun save(entity: ProfilTokoEntity)
}
