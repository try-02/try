package com.sentral.org.data.model

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.fail

class SuspendRunCatchingTest {

    @Test
    fun suksesDibungkusResultSuccess() = runTest {
        assertEquals(42, suspendRunCatching { 42 }.getOrThrow())
    }

    @Test
    fun exceptionBiasaMenjadiResultFailureDenganInstansYangSama() = runTest {
        val asli = IllegalStateException("boom")
        val hasil = suspendRunCatching<Int> { throw asli }
        assertSame(asli, hasil.exceptionOrNull())
    }

    @Test
    fun cancellationExceptionDiteruskanBukanDitelan() = runTest {
        try {
            suspendRunCatching<Unit> { throw CancellationException("pekerjaan dibatalkan") }
            fail("CancellationException harus propagate")
        } catch (_: CancellationException) {
            // sesuai harapan
        }
    }
}