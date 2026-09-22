package com.sentral.org.data.entity

import androidx.room3.Embedded
import androidx.room3.Relation

data class TransaksiDenganDetail(
    @Embedded
    val transaksi: TransaksiEntity,

    @Relation(
        parentColumns = ["id"],
        entityColumns = ["transaksi_id"]
    )
    val items: List<ItemTransaksiEntity>,

    @Relation(
        parentColumns = ["id"],
        entityColumns = ["transaksi_id"]
    )
    val pembayaran: List<PembayaranEntity>
)