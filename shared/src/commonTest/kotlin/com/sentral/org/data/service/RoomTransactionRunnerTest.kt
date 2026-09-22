package com.sentral.org.data.service

import com.sentral.org.data.PosDatabase
import com.sentral.org.data.createInMemoryPosDatabase
import com.sentral.org.data.entity.KasirEntity
import com.sentral.org.data.entity.PergerakanKasEntity
import com.sentral.org.data.entity.ShiftEntity
import com.sentral.org.data.model.JenisPergerakanKas
import com.sentral.org.data.model.StatusShift
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RoomTransactionRunnerTest {

    private lateinit var db: PosDatabase
    private lateinit var runner: RoomTransactionRunner

    @BeforeTest
    fun setUp() {
        db = createInMemoryPosDatabase()
        runner = RoomTransactionRunner(db)
    }

    @AfterTest
    fun tearDown() {
        db.close()
    }

    @Test
    fun suksesMengcommitSeluruhTulisan() = runTest {
        val (kasirId, shiftId) = runner.run {
            val k = db.kasirDao().insert(
                KasirEntity(nama = "Kasir Commit", pinHash = null, aktif = true, dibuatPada = 1)
            )
            val s = db.shiftDao().insert(
                ShiftEntity(
                    kasirId = k, namaKasir = "Kasir Commit", status = StatusShift.TERBUKA,
                    kasAwal = 5, dimulaiPada = 1, kasDiharapkan = null, kasAktual = null,
                    selisihKas = null, ditutupPada = null, catatan = "",
                )
            )
            k to s
        }
        assertNotNull(db.kasirDao().getById(kasirId))
        assertNotNull(db.shiftDao().getById(shiftId))
    }

    @Test
    fun gagalDiTengahJalanMembuangSeluruhTulisanParsial() = runTest {
        var kasirId = 0L
        var shiftId = 0L

        val hasil = runCatching {
            runner.run {
                kasirId = db.kasirDao().insert(
                    KasirEntity(nama = "Kasir Rollback", pinHash = null, aktif = true, dibuatPada = 1)
                )
                shiftId = db.shiftDao().insert(
                    ShiftEntity(
                        kasirId = kasirId, namaKasir = "Kasir Rollback", status = StatusShift.TERBUKA,
                        kasAwal = 5, dimulaiPada = 1, kasDiharapkan = null, kasAktual = null,
                        selisihKas = null, ditutupPada = null, catatan = "",
                    )
                )
                db.pergerakanKasDao().insert(
                    PergerakanKasEntity(
                        shiftId = shiftId, jenis = JenisPergerakanKas.KAS_AWAL, jumlahDelta = 5,
                        transaksiId = null, pengembalianId = null, keterangan = "parsial",
                        dibuatPada = 1,
                    )
                )
                error("Ledakan simulasi di tengah transaksi")
            }
        }

        assertTrue(hasil.isFailure)
        assertNull(db.kasirDao().getById(kasirId), "kasir harus ikut ter-roll-back")
        assertNull(db.shiftDao().getById(shiftId), "shift harus ikut ter-roll-back")
        assertTrue(db.kasirDao().observeAktif().first().isEmpty(), "tidak boleh ada kasir tersisa")
    }
}