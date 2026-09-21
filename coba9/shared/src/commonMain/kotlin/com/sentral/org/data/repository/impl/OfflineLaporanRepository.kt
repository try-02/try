package com.sentral.org.data.repository.impl

import com.sentral.org.data.dao.ReturDao
import com.sentral.org.data.dao.TransaksiDao
import com.sentral.org.data.model.LaporanCalculator
import com.sentral.org.data.model.LaporanPenjualan
import com.sentral.org.data.repository.LaporanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class OfflineLaporanRepository(
    private val transaksiDao: TransaksiDao,
    private val returDao: ReturDao,
) : LaporanRepository {

    override fun observeLaporan(start: Long, end: Long): Flow<LaporanPenjualan> {
        return combine(
            transaksiDao.observeSalesAggregate(start, end),
            transaksiDao.observeHppPenjualan(start, end),
            returDao.observeTotalRefund(start, end),
            returDao.observeHppRetur(start, end),
            transaksiDao.observePaymentAggregate(start, end),
            transaksiDao.observeTopProducts(start, end),
        ) { sales, hppSales, totalRefund, hppRetur, payments, topProducts ->
            LaporanCalculator.calculate(
                sales = sales,
                hppPenjualan = hppSales,
                totalRetur = totalRefund,
                hppRetur = hppRetur,
                payments = payments,
                topProducts = topProducts,
            )
        }
    }
}