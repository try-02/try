package com.sentral.org.data.dao

import androidx.room3.Dao
import androidx.room3.Embedded
import androidx.room3.Insert
import androidx.room3.ColumnInfo
import androidx.room3.Query
import com.sentral.org.data.entity.ItemKeranjangEntity
import kotlinx.coroutines.flow.Flow

/** Hasil JOIN keranjang x produk: harga live dari master, bukan snapshot. */
data class BarisKeranjangLive(
    @Embedded val item: ItemKeranjangEntity,
    @ColumnInfo(name = "nama_master") val namaMaster: String,
    @ColumnInfo(name = "harga_master") val hargaMaster: Long,
)

@Dao
interface ItemKeranjangDao {
    @Insert
    suspend fun insert(entity: ItemKeranjangEntity): Long

    @Query("SELECT * FROM item_keranjang WHERE keranjang_id = :cartId ORDER BY id")
    suspend fun getByCart(cartId: Long): List<ItemKeranjangEntity>

    @Query("""
        SELECT i.*, p.nama AS nama_master, p.harga AS harga_master
        FROM item_keranjang i
        JOIN produk p ON p.id = i.produk_id
        WHERE i.keranjang_id = :cartId
        ORDER BY i.id
    """)
    fun observeLiveByCart(cartId: Long): Flow<List<BarisKeranjangLive>>

    @Query("""
        SELECT * FROM item_keranjang
        WHERE keranjang_id = :cartId AND produk_id = :productId LIMIT 1
    """)
    suspend fun getByProduct(cartId: Long, productId: Long): ItemKeranjangEntity?

    @Query("""
        UPDATE item_keranjang SET jumlah = jumlah + :delta, diperbarui_pada = :now
        WHERE keranjang_id = :cartId AND produk_id = :productId AND jumlah + :delta > 0
    """)
    suspend fun changeQuantity(cartId: Long, productId: Long, delta: Long, now: Long): Int

    @Query("DELETE FROM item_keranjang WHERE id = :id")
    suspend fun deleteById(id: Long): Int

    @Query("DELETE FROM item_keranjang WHERE keranjang_id = :cartId AND produk_id = :productId")
    suspend fun deleteByProduct(cartId: Long, productId: Long): Int

    @Query("DELETE FROM item_keranjang WHERE keranjang_id = :cartId")
    suspend fun deleteByCart(cartId: Long): Int
}