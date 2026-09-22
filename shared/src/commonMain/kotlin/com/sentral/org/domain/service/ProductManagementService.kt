package com.sentral.org.domain.service

import com.sentral.org.data.dao.PergerakanPersediaanDao
import com.sentral.org.data.dao.PersediaanDao
import com.sentral.org.data.dao.ProdukDao
import com.sentral.org.data.dao.ProdukDenganStokRaw
import com.sentral.org.data.entity.PergerakanPersediaanEntity
import com.sentral.org.data.entity.PersediaanEntity
import com.sentral.org.data.entity.ProdukEntity
import com.sentral.org.data.model.JenisPergerakanPersediaan
import com.sentral.org.data.model.PosDataException
import com.sentral.org.data.model.formatQuantity
import com.sentral.org.data.model.suspendRunCatching
import com.sentral.org.data.service.InventoryMutationService
import com.sentral.org.data.service.PosWriteService
import com.sentral.org.domain.model.CreateProductRequest
import com.sentral.org.domain.model.DisposeDamageRequest
import com.sentral.org.domain.model.KartuStokFilter
import com.sentral.org.domain.model.RecordDamageRequest
import com.sentral.org.domain.model.RestockRequest
import com.sentral.org.domain.model.StockOpnameRequest
import com.sentral.org.domain.model.UpdateProductRequest
import kotlinx.coroutines.flow.Flow

class ProductManagementService(
    private val write: PosWriteService,
    private val produkDao: ProdukDao,
    private val persediaanDao: PersediaanDao,
    private val ledgerDao: PergerakanPersediaanDao,
    private val mutationService: InventoryMutationService,
) {
    fun observeKatalogAdmin(
        query: String = "",
        kategori: String? = null,
        hanyaAktif: Boolean = false,
    ): Flow<List<ProdukDenganStokRaw>> = produkDao.observeProdukAdmin(query, kategori, hanyaAktif)

    fun observeKategori(): Flow<List<String>> = produkDao.observeSemuaKategori()

    fun observeKartuStok(filter: KartuStokFilter): Flow<List<PergerakanPersediaanEntity>> =
        ledgerDao.observeKartuStok(
            produkId = filter.produkId,
            startDate = filter.startDate,
            endDate = filter.endDate,
            jenis = filter.jenis,
        )

    suspend fun createProduct(
        request: CreateProductRequest,
        now: Long,
    ): Result<Long> =
        suspendRunCatching {
            val namaClean = request.nama.trim()
            val skuClean = request.sku.trim()
            val barcodeClean = request.barcode?.trim()?.takeIf { it.isNotBlank() }

            require(namaClean.isNotBlank()) { "Nama produk tidak boleh kosong" }
            require(skuClean.isNotBlank()) { "SKU produk tidak boleh kosong" }
            require(request.harga >= 0) { "Harga jual tidak boleh negatif" }
            require(request.hargaModal >= 0) { "Harga modal tidak boleh negatif" }
            require(request.stokAwalScaled >= 0) { "Stok awal tidak boleh negatif" }

            write.run {
                if (produkDao.existsBySku(skuClean)) {
                    throw PosDataException.Duplicate("SKU '$skuClean' sudah digunakan oleh produk lain")
                }
                if (barcodeClean != null && produkDao.existsByBarcode(barcodeClean)) {
                    throw PosDataException.Duplicate("Barcode '$barcodeClean' sudah digunakan oleh produk lain")
                }

                val produkId =
                    produkDao.insert(
                        ProdukEntity(
                            nama = namaClean,
                            sku = skuClean,
                            barcode = barcodeClean,
                            harga = request.harga,
                            hargaModal = request.hargaModal,
                            kategori = request.kategori.trim().ifBlank { "Umum" },
                            aktif = true,
                            dibuatPada = now,
                            diperbaruiPada = now,
                        ),
                    )

                persediaanDao.insert(
                    PersediaanEntity(
                        produkId = produkId,
                        jumlah = request.stokAwalScaled,
                        jumlahRusak = 0L,
                        diperbaruiPada = now,
                    ),
                )

                if (request.stokAwalScaled > 0L) {
                    ledgerDao.insert(
                        PergerakanPersediaanEntity(
                            produkId = produkId,
                            jenis = JenisPergerakanPersediaan.STOK_AWAL,
                            perubahanJumlah = request.stokAwalScaled,
                            perubahanJumlahRusak = 0L,
                            saldoJumlahSebelum = 0L,
                            saldoJumlahSetelah = request.stokAwalScaled,
                            saldoRusakSebelum = 0L,
                            saldoRusakSetelah = 0L,
                            transaksiId = null,
                            itemTransaksiId = null,
                            pengembalianId = null,
                            itemPengembalianId = null,
                            shiftId = request.shiftId,
                            keterangan = "[${request.operator}] Stok awal produk baru",
                            dibuatPada = now,
                        ),
                    )
                }

                produkId
            }
        }

    suspend fun updateProduct(
        request: UpdateProductRequest,
        now: Long,
    ): Result<Unit> =
        suspendRunCatching {
            val namaClean = request.nama.trim()
            val skuClean = request.sku.trim()
            val barcodeClean = request.barcode?.trim()?.takeIf { it.isNotBlank() }

            require(namaClean.isNotBlank()) { "Nama produk tidak boleh kosong" }
            require(skuClean.isNotBlank()) { "SKU produk tidak boleh kosong" }
            require(request.harga >= 0) { "Harga jual tidak boleh negatif" }
            require(request.hargaModal >= 0) { "Harga modal tidak boleh negatif" }

            write.run {
                val existing =
                    produkDao.getById(request.id)
                        ?: throw PosDataException.NotFound("Produk ID ${request.id} tidak ditemukan")

                if (produkDao.existsBySku(skuClean, excludeId = request.id)) {
                    throw PosDataException.Duplicate("SKU '$skuClean' sudah digunakan oleh produk lain")
                }
                if (barcodeClean != null && produkDao.existsByBarcode(barcodeClean, excludeId = request.id)) {
                    throw PosDataException.Duplicate("Barcode '$barcodeClean' sudah digunakan oleh produk lain")
                }

                check(
                    produkDao.updateMaster(
                        id = request.id,
                        nama = namaClean,
                        sku = skuClean,
                        barcode = barcodeClean,
                        harga = request.harga,
                        hargaModal = request.hargaModal,
                        kategori = request.kategori.trim().ifBlank { existing.kategori },
                        waktu = now,
                    ) == 1,
                ) { "Gagal memperbarui data master produk" }
            }
        }

    suspend fun deactivateProduct(
        productId: Long,
        now: Long,
    ): Result<Unit> =
        suspendRunCatching {
            write.run {
                produkDao.getById(productId) ?: throw PosDataException.NotFound("Produk tidak ditemukan")
                check(produkDao.setAktif(productId, false, now) == 1) { "Gagal menonaktifkan produk" }
            }
        }

    suspend fun activateProduct(
        productId: Long,
        now: Long,
    ): Result<Unit> =
        suspendRunCatching {
            write.run {
                produkDao.getById(productId) ?: throw PosDataException.NotFound("Produk tidak ditemukan")
                check(produkDao.setAktif(productId, true, now) == 1) { "Gagal mengaktifkan produk" }
            }
        }

    suspend fun restock(
        request: RestockRequest,
        now: Long,
    ): Result<Unit> =
        suspendRunCatching {
            require(request.jumlahScaled > 0L) { "Jumlah restock harus lebih dari 0" }
            require(request.alasan.trim().isNotBlank()) { "Alasan restock wajib diisi" }

            write.run {
                produkDao.getById(request.produkId) ?: throw PosDataException.NotFound("Produk tidak ditemukan")
                val note = "[${request.operator}] ${request.alasan.trim()}"
                mutationService.mutateNormal(
                    productId = request.produkId,
                    normalDelta = request.jumlahScaled,
                    type = JenisPergerakanPersediaan.STOK_MASUK,
                    allowNegativeStock = false,
                    shiftId = request.shiftId,
                    note = note,
                    now = now,
                )
            }
        }

    suspend fun adjustStockOpname(
        request: StockOpnameRequest,
        now: Long,
    ): Result<Unit> =
        suspendRunCatching {
            require(request.stokFisikScaled >= 0L) { "Stok fisik hasil opname tidak boleh negatif" }
            require(request.alasan.trim().isNotBlank()) { "Alasan stock opname wajib diisi" }

            write.run {
                produkDao.getById(request.produkId) ?: throw PosDataException.NotFound("Produk tidak ditemukan")
                val stokSaatIni = persediaanDao.getByProdukId(request.produkId)?.jumlah ?: 0L
                val delta = request.stokFisikScaled - stokSaatIni

                if (delta == 0L) return@run

                val tanda = if (delta > 0) "+${formatQuantity(delta)}" else formatQuantity(delta)
                val note = "[${request.operator}] Opname (Selisih: $tanda): ${request.alasan.trim()}"

                mutationService.mutateNormal(
                    productId = request.produkId,
                    normalDelta = delta,
                    type = JenisPergerakanPersediaan.PENYESUAIAN,
                    allowNegativeStock = false,
                    shiftId = request.shiftId,
                    note = note,
                    now = now,
                )
            }
        }

    suspend fun recordDamage(
        request: RecordDamageRequest,
        now: Long,
    ): Result<Unit> =
        suspendRunCatching {
            require(request.jumlahScaled > 0L) { "Jumlah barang rusak harus lebih dari 0" }
            require(request.alasan.trim().isNotBlank()) { "Alasan pencatatan barang rusak wajib diisi" }

            write.run {
                produkDao.getById(request.produkId) ?: throw PosDataException.NotFound("Produk tidak ditemukan")
                val note = "[${request.operator}] Barang Rusak: ${request.alasan.trim()}"
                // Transfer: stok normal berkurang, stok rusak bertambah
                mutationService.mutateNormal(
                    productId = request.produkId,
                    normalDelta = -request.jumlahScaled,
                    damagedDelta = request.jumlahScaled,
                    type = JenisPergerakanPersediaan.KERUSAKAN,
                    allowNegativeStock = false,
                    shiftId = request.shiftId,
                    note = note,
                    now = now,
                )
            }
        }

    suspend fun disposeDamage(
        request: DisposeDamageRequest,
        now: Long,
    ): Result<Unit> =
        suspendRunCatching {
            require(request.jumlahScaled > 0L) { "Jumlah pemusnahan harus lebih dari 0" }
            require(request.alasan.trim().isNotBlank()) { "Alasan pemusnahan barang rusak wajib diisi" }

            write.run {
                produkDao.getById(request.produkId) ?: throw PosDataException.NotFound("Produk tidak ditemukan")
                val note = "[${request.operator}] Pemusnahan: ${request.alasan.trim()}"
                // Pemusnahan: stok normal tetap 0, stok rusak berkurang
                mutationService.mutateNormal(
                    productId = request.produkId,
                    normalDelta = 0L,
                    damagedDelta = -request.jumlahScaled,
                    type = JenisPergerakanPersediaan.PEMUSNAHAN,
                    allowNegativeStock = false,
                    shiftId = request.shiftId,
                    note = note,
                    now = now,
                )
            }
        }
}
