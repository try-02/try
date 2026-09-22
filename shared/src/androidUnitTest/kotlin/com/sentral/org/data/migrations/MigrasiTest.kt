package com.sentral.org.data.migrations

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sentral.org.data.migrasi.PosMigrasi
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun placeholder_migrationTestBelumDibutuhkan() {
        // Placeholder agar file test tidak kosong. Hapus saat migration pertama dibuat.
        assertEquals(0, PosMigrasi.ALL.size)
    }
}
