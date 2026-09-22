package com.sentral.org.data.repository

import com.sentral.org.data.dao.BarisKeranjangLive
import com.sentral.org.data.entity.ItemKeranjangEntity
import com.sentral.org.data.entity.KeranjangEntity
import kotlinx.coroutines.flow.Flow

interface CartRepository {
    fun observeOpen(): Flow<List<KeranjangEntity>>
    fun observeItemsLive(cartId: Long): Flow<List<BarisKeranjangLive>>
    suspend fun getById(id: Long): KeranjangEntity?
    suspend fun getItems(cartId: Long): List<ItemKeranjangEntity>
    suspend fun addItem(entity: ItemKeranjangEntity): Long
    suspend fun changeQuantity(cartId: Long, productId: Long, delta: Long, now: Long): Int
    suspend fun deleteItem(itemId: Long): Int
    suspend fun hold(cartId: Long, now: Long): Int
    suspend fun resume(cartId: Long, now: Long): Int
    suspend fun cancel(cartId: Long, now: Long): Int
}