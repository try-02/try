package com.sentral.org.data.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MoneyMathTest {

    // ---------- lineTotal ----------

    @Test
    fun lineTotalPerkalianDasar() {
        // 15.000 x 2.000 unit (2 buah, skala 1000) = 30.000
        assertEquals(30_000L, MoneyMath.lineTotal(unitPrice = 15_000, quantityScaled = 2_000))
    }

    @Test
    fun lineTotalMembulatkanHALF_UPBukanTruncate() {
        // 15 x 0.100 unit = 1.5 -> HALF_UP = 2 (floorDiv lama menghasilkan 1)
        assertEquals(2L, MoneyMath.lineTotal(unitPrice = 15, quantityScaled = 100))
        // 1234 x 0.567 unit = 699.678 -> 700
        assertEquals(700L, MoneyMath.lineTotal(unitPrice = 1_234, quantityScaled = 567))
    }

    @Test
    fun lineTotalPembagianEksakTidakBergeser() {
        assertEquals(1_000L, MoneyMath.lineTotal(unitPrice = 2_500, quantityScaled = 400))
    }

    @Test
    fun lineTotalMenolakInputTidakValid() {
        assertFailsWith<IllegalArgumentException> {
            MoneyMath.lineTotal(unitPrice = -1, quantityScaled = 1_000)
        }
        assertFailsWith<IllegalArgumentException> {
            MoneyMath.lineTotal(unitPrice = 100, quantityScaled = 0)
        }
    }

    // ---------- percentage ----------

    @Test
    fun persentaseHALF_UPPadaPecahan0Koma9() {
        // 10% dari 199.999 = 19.999,9 -> 20.000
        assertEquals(20_000L, MoneyMath.percentage(value = 199_999, scaledPercent = 10_000))
    }

    @Test
    fun persentaseHALF_UPPadaPecahanTepat0Koma5() {
        // 50% dari 12.345 = 6.172,5 -> 6.173
        assertEquals(6_173L, MoneyMath.percentage(value = 12_345, scaledPercent = 50_000))
        // 12,5% dari 100 = 12,5 -> 13
        assertEquals(13L, MoneyMath.percentage(value = 100, scaledPercent = 12_500))
    }

    @Test
    fun persentaseBatas0Dan100() {
        assertEquals(0L, MoneyMath.percentage(value = 77_777, scaledPercent = 0))
        assertEquals(77_777L, MoneyMath.percentage(value = 77_777, scaledPercent = 100_000))
    }

    @Test
    fun persentaseMenolakDiLuarRentang() {
        assertFailsWith<IllegalArgumentException> {
            MoneyMath.percentage(value = 100, scaledPercent = -1)
        }
        assertFailsWith<IllegalArgumentException> {
            MoneyMath.percentage(value = 100, scaledPercent = 100_001)
        }
    }

    // ---------- proportional ----------

    @Test
    fun proportionalKasusReturParsial() {
        // neto baris 101 diretur 1/3 -> 33,666.. -> 34
        assertEquals(34L, MoneyMath.proportional(part = 1, total = 3, amount = 101))
    }

    @Test
    fun proportionalPembagianEksak() {
        assertEquals(50L, MoneyMath.proportional(part = 2, total = 4, amount = 100))
    }

    @Test
    fun proportionalMenolakPembagiNol() {
        assertFailsWith<IllegalArgumentException> {
            MoneyMath.proportional(part = 1, total = 0, amount = 100)
        }
    }

    // ---------- sumExact & overflow ----------

    @Test
    fun sumExactMenjumlahkanBiasa() {
        assertEquals(6L, MoneyMath.sumExact(listOf(1, 2, 3)))
        assertEquals(0L, MoneyMath.sumExact(emptyList()))
    }

    @Test
    fun sumExactMeledakSaatOverflowBukanSilentWrap() {
        assertFailsWith<ArithmeticException> {
            MoneyMath.sumExact(listOf(Long.MAX_VALUE, 1L))
        }
    }

    @Test
    fun multiplyExactDiDalamPerhitunganTetapMenjagaOverflow() {
        assertFailsWith<ArithmeticException> {
            MoneyMath.lineTotal(unitPrice = Long.MAX_VALUE, quantityScaled = 2_000)
        }
    }

    // ---------- konversi kuantitas ----------

    @Test
    fun quantityOfMengonversiUnitUtuhKeSkalaStorage() {
        assertEquals(2_000L, quantityOf(2))
        assertEquals(0L, quantityOf(0))
        assertFailsWith<ArithmeticException> { quantityOf(Long.MAX_VALUE) }
    }

    // ---------- allocateProportional ----------

    @Test
    fun alokasiMerataDenganSisaPembulatanKeFraksiTerbesar() {
        // 10 dibagi 3 bagian identik: 3,33 masing-masing -> satu bagian dapat 4
        val hasil = MoneyMath.allocateProportional(weights = listOf(100, 100, 100), amount = 10)
        assertEquals(listOf(4L, 3L, 3L), hasil)
        assertEquals(10L, hasil.sum())
    }

    @Test
    fun alokasiProporsionalBobotBerbeda() {
        // 105 atas bobot 60:30:10 -> 63 ; 31,5 ; 10,5 -> sisa 1 ke indeks pertama yg seri
        val hasil = MoneyMath.allocateProportional(weights = listOf(600, 300, 100), amount = 105)
        assertEquals(listOf(63L, 32L, 10L), hasil)
        assertEquals(105L, hasil.sum())
    }

    @Test
    fun kasusKlasikBagiPenny() {
        assertEquals(listOf(2L, 1L), MoneyMath.allocateProportional(listOf(50, 50), 3))
    }

    @Test
    fun kasusPatologisBanyakBarisNilaiKecilTetapAman() {
        val hasil = MoneyMath.allocateProportional(List(32) { 1L }, 16)
        assertTrue(hasil.all { it >= 0 })
        assertEquals(16L, hasil.sum())
        assertEquals(16, hasil.count { it == 1L })
    }

    @Test
    fun alokasiNolDanKosong() {
        assertEquals(List(3) { 0L }, MoneyMath.allocateProportional(listOf(5, 5, 5), 0))
        assertTrue(MoneyMath.allocateProportional(emptyList(), 5).isEmpty())
    }

    @Test
    fun alokasiMenolakTotalBobotNolSaatAmountPositif() {
        assertFailsWith<IllegalArgumentException> {
            MoneyMath.allocateProportional(listOf(0L, 0L), 100)
        }
        assertFailsWith<IllegalArgumentException> {
            MoneyMath.allocateProportional(listOf(1L, 1L), -1)
        }
    }

    // ---------- invariant lintas skenario ----------

    @Test
    fun invariantAlokasiPadaKombinasiBobotBeragam() {
        val skenario = listOf(
            listOf(1L),
            listOf(1L, 2L, 3L, 5L, 8L, 13L),
            listOf(9_999L, 1L),
            listOf(333L, 333L, 333L, 1L),
            List(7) { (it + 1) * 111L },
        )
        val amounts = listOf(1L, 7L, 999L, 123_456L)

        for (weights in skenario) {
            for (amount in amounts) {
                val hasil = MoneyMath.allocateProportional(weights, amount)
                assertTrue(hasil.all { it >= 0 }, "negatif pada $weights/$amount: $hasil")
                assertEquals(amount, hasil.sum(), "jumlah rusak pada $weights/$amount: $hasil")
            }
        }
    }
}