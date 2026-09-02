package com.sentral.org.data.model

/**
 * Kebijakan pembulatan uang: HALF_UP konsisten untuk SEMUA pembagi
 * (skala quantity, persentase, dan alokasi proporsional).
 * Overflow dijaga oleh multiplyExact/addExact, bukan silent-wrap.
 */
object MoneyMath {

    fun lineTotal(unitPrice: Long, quantityScaled: Long): Long {
        require(unitPrice >= 0)
        require(quantityScaled > 0)
        return divideHalfUp(Math.multiplyExact(unitPrice, quantityScaled), QUANTITY_SCALE)
    }

    fun percentage(value: Long, scaledPercent: Long): Long {
        require(value >= 0)
        require(scaledPercent in 0..100_000)
        return divideHalfUp(Math.multiplyExact(value, scaledPercent), 100_000L)
    }

    fun proportional(part: Long, total: Long, amount: Long): Long {
        require(part >= 0 && total > 0 && amount >= 0)
        return divideHalfUp(Math.multiplyExact(part, amount), total)
    }

    fun sumExact(values: Iterable<Long>): Long = values.fold(0L, Math::addExact)

    /**
     * Membagi [amount] proporsional ke tiap bobot dengan metode largest-remainder:
     * jumlah hasil SELALU persis [amount] dan tidak ada alokasi negatif, bahkan
     * pada kasus ekstrem (banyak baris bernilai kecil).
     */
    fun allocateProportional(weights: List<Long>, amount: Long): List<Long> {
        require(amount >= 0) { "Nilai alokasi tidak boleh negatif" }
        if (weights.isEmpty()) return emptyList()
        if (amount == 0L) return List(weights.size) { 0L }
        val total = sumExact(weights)
        require(total > 0) { "Total bobot harus > 0 bila alokasi > 0" }

        val remainders = LongArray(weights.size)
        var allocated = 0L
        val result = MutableList(weights.size) { i ->
            val dividend = Math.multiplyExact(amount, weights[i])
            val base = dividend / total
            remainders[i] = dividend % total
            allocated += base
            base
        }
        // Sisa pembulatan (< jumlah baris) diberikan ke baris dgn fraksi terbesar.
        var leftover = amount - allocated
        for (i in weights.indices.sortedByDescending { remainders[it] }) {
            if (leftover-- == 0L) break
            result[i] += 1
        }
        return result
    }

    // Caller menjamin dividend >= 0, sehingga formula ini aman.
    private fun divideHalfUp(dividend: Long, divisor: Long): Long =
        (dividend + divisor / 2) / divisor
}