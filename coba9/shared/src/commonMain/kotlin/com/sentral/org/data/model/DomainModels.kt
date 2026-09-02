package com.sentral.org.data.model

/**
 * Seluruh kuantitas tersimpan (item keranjang, item transaksi, saldo & mutasi
 * persediaan) memakai fixed-point dengan skala ini: 1 buah = 1_000.
 */
const val QUANTITY_SCALE = 1000L

/** 2 buah -> 2_000. Satu-satunya pintu konversi dari input pengguna ke storage. */
fun quantityOf(units: Long): Long = Math.multiplyExact(units, QUANTITY_SCALE)

@JvmInline
value class Quantity(val scaled: Long) {
    init { require(scaled > 0) { "Quantity harus > 0" } }
}

@JvmInline
value class Money(val rupiah: Long) {
    init { require(rupiah >= 0) { "Money tidak boleh negatif" } }
}

sealed interface DiscountInput {
    data object None : DiscountInput
    data class Nominal(val rupiah: Long) : DiscountInput
    /** Percentage scaled by 1000: 10_000 = 10.0%, 12500 = 12.5%, 100000 = 100%. */
    data class Percentage(val scaledPercent: Long) : DiscountInput
}

data class CartLine(
    val productId: Long,
    val name: String,
    val unitPrice: Long,
    val quantity: Long,
    val unitCost: Long,
)

data class CheckoutRequest(
    val cartId: Long,
    val cashierId: Long,
    val shiftId: Long,
    val payments: List<PaymentRequest>,
    val discount: DiscountInput = DiscountInput.None,
    val tax: Long = 0,
    val transactionNumber: String,
    val now: Long,
    val warrantyExchange: Boolean = false,
)

data class PaymentRequest(
    val method: MetodePembayaran,
    val amount: Long,
    val received: Long? = null,
    val reference: String? = null,
)

data class CheckoutResult(
    val transactionId: Long,
    val transactionNumber: String,
    val subtotal: Long,
    val discount: Long,
    val tax: Long,
    val total: Long,
    val paid: Long,
    val change: Long,
)

data class ReturnLineRequest(
    val transactionItemId: Long,
    val quantity: Long,
    val destination: TujuanStokPengembalian,
)

data class ReturnRequest(
    val transactionId: Long,
    val cashierId: Long,
    val shiftId: Long?,
    val lines: List<ReturnLineRequest>,
    val refundMethod: MetodePembayaran,
    val note: String = "",
    val replacementTransactionId: Long? = null,
    val warrantyExchange: Boolean = false,
    val now: Long,
)

data class ReturnResult(
    val returnId: Long,
    val refundAmount: Long,
)

data class VoidRequest(
    val transactionId: Long,
    val cashierId: Long,
    val shiftId: Long?,
    val reason: String,
    val now: Long,
)