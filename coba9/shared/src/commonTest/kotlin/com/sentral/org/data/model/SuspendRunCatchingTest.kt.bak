package com.sentral.org.data.model

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test

class SuspendRunCatchingTest {

    @Test
    fun `sukses dibungkus Result success`() = runBlocking {
        assertEquals(42, suspendRunCatching { 42 }.getOrThrow())
    }

    @Test
    fun `exception biasa menjadi Result failure dengan instans yang sama`() = runBlocking {
        val asli = IllegalStateException("boom")
        val hasil = suspendRunCatching<Int> { throw asli }
        assertSame(asli, hasil.exceptionOrNull())
    }

    @Test
    fun `CancellationException DITERUSKAN bukan ditelan`() = runBlocking {
        try {
            suspendRunCatching<Unit> { throw CancellationException("pekerjaan dibatalkan") }
            fail("CancellationException harus propagate")
        } catch (_: CancellationException) {
            // sesuai harapan
        }
    }
}