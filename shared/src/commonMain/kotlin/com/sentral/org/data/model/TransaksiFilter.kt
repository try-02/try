package com.sentral.org.data.model

/**
 * Filter immutable untuk pencarian, paging, dan ekspor riwayat transaksi.
 * @param query Nomor transaksi (menggunakan prefix search)
 * @param status Status transaksi (null = SEMUA)
 * @param startDate Timestamp awal milidetik (inklusif, null = tanpa batas)
 * @param endDate Timestamp akhir milidetik (inklusif, null = tanpa batas)
 */
data class TransaksiFilter(
    val query: String = "",
    val status: StatusTransaksi? = null,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val refreshEpoch: Long = 0L,
)