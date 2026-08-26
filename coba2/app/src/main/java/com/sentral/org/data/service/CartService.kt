package com.sentral.org.data.service

import com.sentral.org.data.dao.ItemKeranjangDao
import com.sentral.org.data.dao.KasirDao
import com.sentral.org.data.dao.KeranjangDao
import com.sentral.org.data.dao.ProdukDao
import com.sentral.org.data.entity.ItemKeranjangEntity
import com.sentral.org.data.entity.KeranjangEntity
import com.sentral.org.data.model.PosDataException
import com.sentral.org.data.model.StatusKeranjang
import com.sentral.org.data.model.suspendRunCatching

class CartService(
    private val write: PosWriteService,
    private val carts: KeranjangDao,
    private val items: ItemKeranjangDao,
    private val products: ProdukDao,
    private val cashiers: KasirDao,
) {
    suspend fun buatKeranjang(kasirId: Long, now: Long): Result<Long> = suspendRunCatching {
        write.run {
            val kasir = cashiers.getById(kasirId)
                ?: throw PosDataException.NotFound("Kasir tidak ditemukan")
            if (!kasir.aktif) throw PosDataException.Validation("Kasir tidak aktif")
            carts.insert(
                KeranjangEntity(
                    nama = "Keranjang",
                    status = StatusKeranjang.AKTIF,
                    kasirId = kasir.id,
                    namaKasir = kasir.nama,
                    dibuatPada = now,
                    diperbaruiPada = now,
                    ditahanPada = null,
                    diselesaikanPada = null,
                    dibatalkanPada = null,
                )
            )
        }
    }

    /** @param quantity TER-SKALA QUANTITY_SCALE — gunakan quantityOf(2) untuk 2 buah. */
    suspend fun addProduct(cartId: Long, productId: Long, quantity: Long, now: Long): Result<Unit> =
        suspendRunCatching {
            require(quantity > 0)
            write.run { tulisDelta(cartId, productId, quantity, now) }
        }

    /** @param delta TER-SKALA, boleh negatif. Jika hasil <= 0, baris otomatis dihapus. */
    suspend fun ubahJumlah(cartId: Long, productId: Long, delta: Long, now: Long): Result<Unit> =
        suspendRunCatching {
            require(delta != 0L)
            write.run { tulisDelta(cartId, productId, delta, now) }
        }

    suspend fun hapusBaris(cartId: Long, productId: Long, now: Long): Result<Unit> =
        suspendRunCatching {
            write.run {
                pastikanAktif(cartId)
                items.deleteByProduct(cartId, productId)
                Unit
            }
        }

    suspend fun hold(cartId: Long, now: Long): Result<Unit> = transition { carts.hold(cartId, now) }
    suspend fun resume(cartId: Long, now: Long): Result<Unit> = transition { carts.resume(cartId, now) }
    suspend fun cancel(cartId: Long, now: Long): Result<Unit> = transition { carts.cancel(cartId, now) }

    /**
     * Satu jalur untuk tambah/kurang: UPDATE dahulu (transaksi tulis = penulis tunggal).
     * 0 baris terdampak berarti: delta negatif -> baris habis, hapus;
     * delta positif -> item belum ada, INSERT baru.
     */
    private suspend fun tulisDelta(cartId: Long, productId: Long, delta: Long, now: Long) {
        pastikanAktif(cartId)
        val product = products.getById(productId)
            ?: throw PosDataException.NotFound("Produk tidak ditemukan")
        if (!product.aktif) throw PosDataException.Validation("Produk tidak aktif")

        val updated = items.changeQuantity(cartId, productId, delta, now)
        if (updated == 0) {
            if (delta < 0) {
                items.deleteByProduct(cartId, productId)
            } else {
                items.insert(
                    ItemKeranjangEntity(
                        keranjangId = cartId,
                        produkId = productId,
                        namaProduk = product.nama,
                        hargaSatuan = product.harga,
                        jumlah = delta,
                        ditambahkanPada = now,
                        diperbaruiPada = now,
                    )
                )
            }
        }
    }

    private suspend fun pastikanAktif(cartId: Long) {
        val cart = carts.getById(cartId)
            ?: throw PosDataException.NotFound("Keranjang tidak ditemukan")
        if (cart.status != StatusKeranjang.AKTIF) {
            throw PosDataException.InvalidState("Keranjang harus AKTIF")
        }
    }

    private suspend fun transition(operation: suspend () -> Int): Result<Unit> = suspendRunCatching {
        write.run { check(operation() == 1) { "Status keranjang sudah berubah" } }
    }
}