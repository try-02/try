package com.sentral.org.data.service

import com.sentral.org.data.dao.*
import com.sentral.org.data.entity.*
import com.sentral.org.data.model.*

class CheckoutService(
    private val write: PosWriteService,
    private val products: ProdukDao,
    private val carts: KeranjangDao,
    private val cartItems: ItemKeranjangDao,
    private val cashiers: KasirDao,
    private val shifts: ShiftDao,
    private val transactions: TransaksiDao,
    private val transactionItems: ItemTransaksiDao,
    private val payments: PembayaranDao,
    private val cashLedger: PergerakanKasDao,
    private val inventory: InventoryMutationService,
) {
    /**
     * Seluruh proses (validasi, komputasi, penulisan) dieksekusi di dalam
     * SATU transaksi tulis: konsistensi penuh tanpa window baca->tulis.
     */
    suspend fun checkout(request: CheckoutRequest): Result<CheckoutResult> = suspendRunCatching {
        write.run {
            // ---------- VALIDASI ----------
            val cart = carts.getById(request.cartId)
                ?: throw PosDataException.NotFound("Keranjang tidak ditemukan")
            if (cart.status != StatusKeranjang.AKTIF) {
                throw PosDataException.InvalidState("Keranjang bukan AKTIF")
            }
            val cashier = cashiers.getById(request.cashierId)
                ?: throw PosDataException.NotFound("Kasir tidak ditemukan")
            if (!cashier.aktif) throw PosDataException.Validation("Kasir tidak aktif")
            val shift = shifts.getById(request.shiftId)
                ?: throw PosDataException.NotFound("Shift tidak ditemukan")
            if (shift.status != StatusShift.TERBUKA || shift.kasirId != request.cashierId) {
                throw PosDataException.InvalidState("Shift tidak aktif atau bukan milik kasir")
            }

            // ---------- KOMPUTASI ----------
            val cartRows = cartItems.getByCart(cart.id)
            if (cartRows.isEmpty()) throw PosDataException.Validation("Keranjang kosong")

            // Harga selalu diambil dari master produk saat checkout,
            // snapshot harga keranjang sengaja diabaikan.
            val lines = cartRows.map { row ->
                val product = products.getById(row.produkId)
                    ?: throw PosDataException.NotFound("Produk ${row.produkId} tidak ditemukan")
                if (!product.aktif) throw PosDataException.ProductInactive(product.id, product.nama)
                if (row.jumlah <= 0) throw PosDataException.Validation("Quantity tidak valid")
                CartLine(product.id, product.nama, product.harga, row.jumlah, product.hargaModal)
            }

            val lineSubtotals = lines.map { MoneyMath.lineTotal(it.unitPrice, it.quantity) }
            val subtotal = MoneyMath.sumExact(lineSubtotals)
            val discountTotal = calculateDiscount(subtotal, request.discount)
            val tax = request.tax.coerceAtLeast(0)
            val total = subtotal - discountTotal + tax

            val paymentCalculation = validatePayments(request.payments, total, request.now)
            val allocatedDiscounts = MoneyMath.allocateProportional(lineSubtotals, discountTotal)

            // ---------- TULIS ----------
            val transactionId = transactions.insert(
                TransaksiEntity(
                    nomorTransaksi = request.transactionNumber,
                    kasirId = cashier.id,
                    namaKasir = cashier.nama,
                    shiftId = shift.id,
                    dibuatPada = request.now,
                    subtotal = subtotal,
                    diskon = discountTotal,
                    pajak = tax,
                    total = total,
                    jenisDiskon = when (request.discount) {
                        DiscountInput.None -> JenisDiskon.NOMINAL
                        is DiscountInput.Nominal -> JenisDiskon.NOMINAL
                        is DiscountInput.Percentage -> JenisDiskon.PERSENTASE
                    },
                    nilaiDiskon = when (request.discount) {
                        DiscountInput.None -> 0
                        is DiscountInput.Nominal -> request.discount.rupiah
                        is DiscountInput.Percentage -> request.discount.scaledPercent
                    },
                    status = StatusTransaksi.SELESAI,
                    dibatalkanPada = null,
                    alasanPembatalan = null,
                    adalahTukarGaransi = request.warrantyExchange,
                ),
            )

            val itemIds = transactionItems.insertAll(
                lines.mapIndexed { index, line ->
                    ItemTransaksiEntity(
                        transaksiId = transactionId,
                        produkId = line.productId,
                        namaProduk = line.name,
                        hargaSatuan = line.unitPrice,
                        jumlah = line.quantity,
                        totalBaris = lineSubtotals[index],
                        diskonItem = allocatedDiscounts[index],
                        hargaModal = line.unitCost,
                    )
                },
            )

            lines.forEachIndexed { index, line ->
                inventory.mutateNormal(
                    productId = line.productId,
                    normalDelta = -line.quantity,
                    type = JenisPergerakanPersediaan.PENJUALAN,
                    transactionId = transactionId,
                    transactionItemId = itemIds[index],
                    shiftId = shift.id,
                    note = "Penjualan ${request.transactionNumber}",
                    now = request.now,
                )
            }

            payments.insertAll(
                paymentCalculation.rows.map { it.copy(transaksiId = transactionId) },
            )

            paymentCalculation.rows.forEach { payment ->
                if (payment.metode == MetodePembayaran.CASH) {
                    cashLedger.insert(
                        PergerakanKasEntity(
                            shiftId = shift.id,
                            jenis = JenisPergerakanKas.PENJUALAN,
                            jumlahDelta = payment.jumlah,
                            transaksiId = transactionId,
                            pengembalianId = null,
                            keterangan = "Penjualan ${request.transactionNumber}",
                            dibuatPada = request.now,
                        ),
                    )
                }
            }

            check(carts.complete(cart.id, request.now) == 1) {
                "Status keranjang berubah saat checkout"
            }

            CheckoutResult(
                transactionId = transactionId,
                transactionNumber = request.transactionNumber,
                subtotal = subtotal,
                discount = discountTotal,
                tax = tax,
                total = total,
                paid = paymentCalculation.paid,
                change = paymentCalculation.change,
            )
        }
    }

    private fun calculateDiscount(subtotal: Long, input: DiscountInput): Long = when (input) {
        DiscountInput.None -> 0L
        is DiscountInput.Nominal -> input.rupiah.coerceIn(0, subtotal)
        is DiscountInput.Percentage -> MoneyMath.percentage(subtotal, input.scaledPercent)
    }
/*
    private fun allocateDiscounts(lines: List<Long>, discount: Long): List<Long> {
        if (discount == 0L) return List(lines.size) { 0L }
        val total = MoneyMath.sumExact(lines)
        val result = MutableList(lines.size) { 0L }
        var allocated = 0L
        lines.forEachIndexed { index, line ->
            result[index] = if (index == lines.lastIndex) {
                discount - allocated // koreksi sisa pembulatan di baris terakhir
            } else {
                MoneyMath.proportional(part = line, total = total, amount = discount)
            }
            allocated += result[index]
        }
        return result
    }
*/
    private data class PaymentCalculation(
        val rows: List<PembayaranEntity>,
        val paid: Long,
        val change: Long,
    )

    private fun validatePayments(
        inputs: List<PaymentRequest>,
        total: Long,
        currentPaymentTimestamp: Long,
    ): PaymentCalculation {
        if (inputs.isEmpty()) throw PosDataException.Validation("Pembayaran kosong")
        if (inputs.any { it.amount <= 0 }) throw PosDataException.Validation("Jumlah pembayaran harus > 0")
        val sum = MoneyMath.sumExact(inputs.map { it.amount })
        if (sum != total) throw PosDataException.Validation("Total pembayaran harus sama dengan total transaksi")

        val rows = inputs.map { input ->
            when (input.method) {
                MetodePembayaran.CASH -> {
                    val received = input.received
                        ?: throw PosDataException.Validation("Pembayaran CASH membutuhkan jumlah diterima")
                    if (received < input.amount) throw PosDataException.Validation("Uang CASH kurang")
                    PembayaranEntity(
                        transaksiId = 0,
                        metode = MetodePembayaran.CASH,
                        jumlah = input.amount,
                        diterima = received,
                        kembalian = received - input.amount,
                        referensi = null,
                        dibuatPada = currentPaymentTimestamp,
                    )
                }
                MetodePembayaran.QRIS -> {
                    if (input.received != null && input.received != input.amount) {
                        throw PosDataException.Validation("QRIS harus dibayar tepat sesuai jumlah")
                    }
                    PembayaranEntity(
                        transaksiId = 0,
                        metode = MetodePembayaran.QRIS,
                        jumlah = input.amount,
                        diterima = null,
                        kembalian = 0,
                        referensi = input.reference,
                        dibuatPada = currentPaymentTimestamp,
                    )
                }
            }
        }
        // "paid" = uang yang benar-benar diserahkan (CASH: received; QRIS: jumlah tagihan),
        // sehingga invariant struk selalu berlaku: paid - total == change.
        val paid = MoneyMath.sumExact(inputs.map { it.received ?: it.amount })
        val cashChange = inputs.filter { it.method == MetodePembayaran.CASH }
            .sumOf { (it.received ?: it.amount) - it.amount }
        return PaymentCalculation(rows, paid, cashChange)
    }
}