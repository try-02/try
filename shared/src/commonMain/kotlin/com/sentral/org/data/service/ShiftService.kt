package com.sentral.org.data.service

import com.sentral.org.data.dao.*
import com.sentral.org.data.entity.PergerakanKasEntity
import com.sentral.org.data.entity.ShiftEntity
import com.sentral.org.data.model.*

class ShiftService(
    private val write: PosWriteService,
    private val cashiers: KasirDao,
    private val shifts: ShiftDao,
    private val cashLedger: PergerakanKasDao,
    private val transactions: TransaksiDao,
    private val payments: PembayaranDao,
) {
    suspend fun open(
        cashierId: Long,
        openingCash: Long,
        now: Long,
        note: String = "",
    ): Result<Long> =
        suspendRunCatching {
            require(openingCash >= 0)
            write.run {
                val cashier =
                    cashiers.getById(cashierId)
                        ?: throw PosDataException.NotFound("Kasir tidak ditemukan")
                if (!cashier.aktif) throw PosDataException.Validation("Kasir tidak aktif")
                if (shifts.hasOpenForKasir(cashierId)) {
                    throw PosDataException.Duplicate("Kasir sudah memiliki shift terbuka")
                }
                val id =
                    shifts.insert(
                        ShiftEntity(
                            kasirId = cashier.id,
                            namaKasir = cashier.nama,
                            status = StatusShift.TERBUKA,
                            kasAwal = openingCash,
                            dimulaiPada = now,
                            kasDiharapkan = null,
                            kasAktual = null,
                            selisihKas = null,
                            ditutupPada = null,
                            catatan = note,
                        ),
                    )
                cashLedger.insert(
                    PergerakanKasEntity(
                        shiftId = id,
                        jenis = JenisPergerakanKas.KAS_AWAL,
                        jumlahDelta = openingCash,
                        transaksiId = null,
                        pengembalianId = null,
                        keterangan = "Kas awal",
                        dibuatPada = now,
                    ),
                )
                id
            }
        }

    /**
     * Mengumpulkan ringkasan angka finansial shift untuk Laporan X atau rekonsiliasi Laporan Z.
     */
    suspend fun getShiftSummary(
        shiftId: Long,
        isZReport: Boolean,
        actualCash: Long? = null,
    ): ShiftSummary {
        val shift =
            shifts.getById(shiftId)
                ?: throw PosDataException.NotFound("Shift tidak ditemukan")

        val expectedCash = cashLedger.getExpectedCash(shiftId)
        val difference = actualCash?.let { it - expectedCash }

        // Ambil data transaksi pada shift ini
        val trxList = transactions.getByShift(shiftId)
        val validTrx = trxList.filter { it.status == StatusTransaksi.SELESAI }

        var penjualanTunai = 0L
        var penjualanNonTunai = 0L

        for (trx in validTrx) {
            val paymentList = payments.getByTransaction(trx.id)
            for (p in paymentList) {
                if (p.metode == MetodePembayaran.CASH) {
                    penjualanTunai += p.jumlah
                } else {
                    penjualanNonTunai += p.jumlah
                }
            }
        }

        // Ambil mutasi retur tunai dari buku kas shift ini
        val kasMovements = cashLedger.getByShift(shiftId)
        val returTunai =
            kasMovements
                .filter { it.jenis == JenisPergerakanKas.RETUR }
                .sumOf { -it.jumlahDelta } // dikonversi ke positif untuk ringkasan

        return ShiftSummary(
            shiftId = shift.id,
            kasirId = shift.kasirId,
            kasirNama = shift.namaKasir,
            dimulaiPada = shift.dimulaiPada,
            ditutupPada = shift.ditutupPada,
            kasAwal = shift.kasAwal,
            totalPenjualanTunai = penjualanTunai,
            totalPenjualanNonTunai = penjualanNonTunai,
            totalReturTunai = returTunai,
            kasDiharapkan = expectedCash,
            kasAktual = actualCash ?: shift.kasAktual,
            selisihKas = difference ?: shift.selisihKas,
            jumlahTransaksi = validTrx.size,
            isZReport = isZReport,
        )
    }

    /**
     * Menutup shift secara atomik dalam satu transaksi tulis:
     * Menghitung kas diharapkan, mencatat selisih fisik kasir, dan mengunci status shift.
     */
    suspend fun close(
        shiftId: Long,
        actualCash: Long,
        now: Long,
        note: String = "",
    ): Result<ShiftSummary> =
        suspendRunCatching {
            require(actualCash >= 0)
            write.run {
                val shift =
                    shifts.getById(shiftId)
                        ?: throw PosDataException.NotFound("Shift tidak ditemukan")
                if (shift.status != StatusShift.TERBUKA) {
                    throw PosDataException.InvalidState("Shift sudah ditutup sebelumnya")
                }
                val expected = cashLedger.getExpectedCash(shiftId)
                val difference = actualCash - expected
                check(shifts.close(shiftId, expected, actualCash, difference, now, note) == 1) {
                    "Gagal memperbarui status shift"
                }
            }
            // Ambil summary final Z Report
            getShiftSummary(shiftId = shiftId, isZReport = true, actualCash = actualCash)
        }
}
