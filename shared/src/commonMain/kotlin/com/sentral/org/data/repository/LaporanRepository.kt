package com.sentral.org.data.repository

import com.sentral.org.data.model.LaporanPenjualan
import kotlinx.coroutines.flow.Flow

interface LaporanRepository {
    fun observeLaporan(start: Long, end: Long): Flow<LaporanPenjualan>
}