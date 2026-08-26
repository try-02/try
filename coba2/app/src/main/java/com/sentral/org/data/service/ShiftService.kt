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
) {
    // CATATAN: pertimbangkan validasi kepemilikan (kasir pembuka vs penutup)
    // bila perangkat dipakai bergantian oleh beberapa kasir.
    suspend fun open(cashierId: Long, openingCash: Long, now: Long, note: String = ""): Result<Long> =
        suspendRunCatching {
            require(openingCash >= 0)
            write.run {
                val cashier = cashiers.getById(cashierId)
                    ?: throw PosDataException.NotFound("Kasir tidak ditemukan")
                if (!cashier.aktif) throw PosDataException.Validation("Kasir tidak aktif")
                if (shifts.hasOpenForKasir(cashierId)) {
                    throw PosDataException.Duplicate("Kasir sudah memiliki shift terbuka")
                }
                val id = shifts.insert(
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
                    )
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
                    )
                )
                id
            }
        }

    suspend fun close(shiftId: Long, actualCash: Long, now: Long, note: String = ""): Result<Unit> =
        suspendRunCatching {
            require(actualCash >= 0)
            write.run {
                val shift = shifts.getById(shiftId)
                    ?: throw PosDataException.NotFound("Shift tidak ditemukan")
                if (shift.status != StatusShift.TERBUKA) {
                    throw PosDataException.InvalidState("Shift sudah ditutup")
                }
                val expected = cashLedger.getExpectedCash(shiftId)
                val difference = actualCash - expected
                check(shifts.close(shiftId, expected, actualCash, difference, now, note) == 1)
            }
        }
}