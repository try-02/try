package com.sentral.org.data.repository.impl

import com.sentral.org.data.dao.PaymentAggregateRaw
import com.sentral.org.data.dao.ReturDao
import com.sentral.org.data.dao.SalesAggregateRaw
import com.sentral.org.data.dao.TopProductRaw
import com.sentral.org.data.dao.TransaksiDao
import com.sentral.org.data.model.LaporanCalculator
import com.sentral.org.data.model.LaporanPenjualan
import com.sentral.org.data.repository.LaporanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

private data class FinancialRaw(
    val sales: SalesAggregateRaw,
    val hppPenjualan: Long,
    val totalRefund: Long,
    val hppRetur: Long,
)

private data class BreakdownRaw(
    val payments: List<PaymentAggregateRaw>,
    val topProducts: List<TopProductRaw>,
)

class OfflineLaporanRepository(
    private val transaksiDao: TransaksiDao,
    private val returDao: ReturDao,
) : LaporanRepository {

    override fun observeLaporan(start: Long, end: Long): Flow<LaporanPenjualan> {
        // Kelompok 1: Metrik finansial agregat (4 flow <= 5)
        val financialFlow = combine(
            transaksiDao.observeSalesAggregate(start, end),
            transaksiDao.observeHppPenjualan(start, end),
            returDao.observeTotalRefund(start, end),
            returDao.observeHppRetur(start, end),
        ) { sales, hppSales, totalRefund, hppRetur ->
            FinancialRaw(
                sales = sales,
                hppPenjualan = hppSales,
                totalRefund = totalRefund,
                hppRetur = hppRetur,
            )
        }

        // Kelompok 2: Rincian metode pembayaran & top produk (2 flow <= 5)
        val breakdownFlow = combine(
            transaksiDao.observePaymentAggregate(start, end),
            transaksiDao.observeTopProducts(start, end),
        ) { payments, topProducts ->
            BreakdownRaw(
                payments = payments,
                topProducts = topProducts,
            )
        }

        // Gabungkan 2 sub-grup secara type-safe ke LaporanCalculator
        return combine(financialFlow, breakdownFlow) { fin, brk ->
            LaporanCalculator.calculate(
                sales = fin.sales,
                hppPenjualan = fin.hppPenjualan,
                totalRetur = fin.totalRefund,
                hppRetur = fin.hppRetur,
                payments = brk.payments,
                topProducts = brk.topProducts,
            )
        }
    }
}