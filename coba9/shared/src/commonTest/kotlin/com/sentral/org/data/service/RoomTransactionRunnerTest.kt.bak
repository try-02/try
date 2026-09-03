package com.sentral.org.data.service

import androidx.room3.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sentral.org.data.PosDatabase
import com.sentral.org.data.entity.KasirEntity
import com.sentral.org.data.entity.PergerakanKasEntity
import com.sentral.org.data.entity.ShiftEntity
import com.sentral.org.data.model.JenisPergerakanKas
import com.sentral.org.data.model.StatusShift
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Menguji mekanisme inti di balik atomicitas semua service:
 * RoomTransactionRunner harus commit penuh saat sukses dan membuang
 * SELURUH tulisan parsial saat exception dilempar di tengah blok.
 */
@RunWith(AndroidJUnit4::class)
class RoomTransactionRunnerTest {

    private lateinit var db: PosDatabase
    private lateinit var runner: RoomTransactionRunner

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, PosDatabase::class.java).build()
        runner = RoomTransactionRunner(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun suksesMengcommitSeluruhTulisan() = runBlocking {
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
    fun gagalDiTengahJalanMembuangSeluruhTulisanParsial() = runBlocking {
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
                // Ledakan SETELAH 3 baris tertulis di dalam transaksi yang sama.
                error("Ledakan simulasi di tengah transaksi")
            }
        }

        assertTrue(hasil.isFailure)
        assertNull("kasir harus ikut ter-roll-back", db.kasirDao().getById(kasirId))
        assertNull("shift harus ikut ter-roll-back", db.shiftDao().getById(shiftId))
        assertTrue("tidak boleh ada kasir tersisa", db.kasirDao().observeAktif().first().isEmpty())
    }
}