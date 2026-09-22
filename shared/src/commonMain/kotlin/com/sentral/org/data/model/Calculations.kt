package com.sentral.org.data.model

/**
 * Kebijakan pembulatan uang: HALF_UP konsisten untuk SEMUA pembagi
 * (skala quantity, persentase, dan alokasi proporsional).
 * Overflow dijaga oleh multiplyExact/addExact murni KMP, bukan silent-wrap.
 */
object MoneyMath {

    fun lineTotal(unitPrice: Long, quantityScaled: Long): Long {
        require(unitPrice >= 0)
        require(quantityScaled > 0)
        return divideHalfUp(multiplyExact(unitPrice, quantityScaled), QUANTITY_SCALE)
    }

    fun percentage(value: Long, scaledPercent: Long): Long {
        require(value >= 0)
        require(scaledPercent in 0..100_000)
        return divideHalfUp(multiplyExact(value, scaledPercent), 100_000L)
    }

    fun proportional(part: Long, total: Long, amount: Long): Long {
        require(part >= 0 && total > 0 && amount >= 0)
        return divideHalfUp(multiplyExact(part, amount), total)
    }

    fun sumExact(values: Iterable<Long>): Long = values.fold(0L) { acc, v -> addExact(acc, v) }

    /**
     * Membagi [amount] proporsional ke tiap bobot dengan metode largest-remainder:
     * jumlah hasil SELALU persis [amount] dan tidak ada alokasi negatif, bahkan
     * pada kasus ekstrem (banyak baris bernilai kecil).
     */
    fun allocateProportional(weights: List<Long>, amount: Long): List<Long> {
        require(amount >= 0) { "Nilai alokasi tidak boleh negatif" }
        if (weights.isEmpty()) return emptyList()
        require(weights.all { it >= 0 }) { "Bobot tidak boleh negatif" }
        if (amount == 0L) return List(weights.size) { 0L }
        val total = sumExact(weights)
        require(total > 0) { "Total bobot harus > 0 bila alokasi > 0" }

        val remainders = LongArray(weights.size)
        var allocated = 0L
        val result = MutableList<Long>(weights.size) { i ->
            val dividend = multiplyExact(amount, weights[i])
            val base = dividend / total
            remainders[i] = dividend % total
            allocated += base
            base
        }
        // Sisa pembulatan (< jumlah baris) diberikan ke baris dgn fraksi terbesar.
        var leftover = amount - allocated
        for (i in weights.indices.sortedByDescending { remainders[it] }) {
            if (leftover-- == 0L) break
            result[i] += 1L
        }
        return result
    }

    // Caller menjamin dividend >= 0, sehingga formula ini aman.
    // addExact mencegah silent overflow bila komputasi bernilai sangat besar.
    private fun divideHalfUp(dividend: Long, divisor: Long): Long =
        addExact(dividend, divisor / 2) / divisor
}

/** Menjumlahkan 2 Long dengan exception saat overflow (KMP replacement untuk Math.addExact). */
internal fun addExact(x: Long, y: Long): Long {
    val r = x + y
    if (((x xor r) and (y xor r)) < 0) {
        throw ArithmeticException("Long overflow: $x + $y")
    }
    return r
}

/** Mengalikan 2 Long dengan exception saat overflow (KMP replacement untuk Math.multiplyExact). */
internal fun multiplyExact(x: Long, y: Long): Long {
    val r = x * y
    val ax = if (x < 0) -x else x
    val ay = if (y < 0) -y else y
    if (((ax or ay) ushr 31) != 0L) {
        if ((y != 0L && r / y != x) || (x == Long.MIN_VALUE && y == -1L)) {
            throw ArithmeticException("Long overflow: $x * $y")
        }
    }
    return r
}