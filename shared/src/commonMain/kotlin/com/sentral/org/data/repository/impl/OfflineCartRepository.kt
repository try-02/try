package com.sentral.org.data.repository.impl

import com.sentral.org.data.dao.BarisKeranjangLive
import com.sentral.org.data.dao.ItemKeranjangDao
import com.sentral.org.data.dao.KeranjangDao
import com.sentral.org.data.entity.ItemKeranjangEntity
import com.sentral.org.data.entity.KeranjangEntity
import com.sentral.org.data.repository.CartRepository
import kotlinx.coroutines.flow.Flow

class OfflineCartRepository(
    private val carts: KeranjangDao,
    private val items: ItemKeranjangDao,
) : CartRepository {
    override fun observeOpen(): Flow<List<KeranjangEntity>> = carts.observeOpen()
    override fun observeItemsLive(cartId: Long): Flow<List<BarisKeranjangLive>> =
        items.observeLiveByCart(cartId)
    override suspend fun getById(id: Long) = carts.getById(id)
    override suspend fun getItems(cartId: Long) = items.getByCart(cartId)
    override suspend fun addItem(entity: ItemKeranjangEntity) = items.insert(entity)
    override suspend fun changeQuantity(cartId: Long, productId: Long, delta: Long, now: Long) =
        items.changeQuantity(cartId, productId, delta, now)
    override suspend fun deleteItem(itemId: Long) = items.deleteById(itemId)
    override suspend fun hold(cartId: Long, now: Long) = carts.hold(cartId, now)
    override suspend fun resume(cartId: Long, now: Long) = carts.resume(cartId, now)
    override suspend fun cancel(cartId: Long, now: Long) = carts.cancel(cartId, now)
}