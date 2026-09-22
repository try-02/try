package com.sentral.org.data.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.sentral.org.data.model.JenisPergerakanPersediaan

@Entity(
    tableName = "pergerakan_persediaan",
    foreignKeys = [
        ForeignKey(
            entity = ProdukEntity::class,
            parentColumns = ["id"],
            childColumns = ["produk_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = TransaksiEntity::class,
            parentColumns = ["id"],
            childColumns = ["transaksi_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = ItemTransaksiEntity::class,
            parentColumns = ["id"],
            childColumns = ["item_transaksi_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = PengembalianEntity::class,
            parentColumns = ["id"],
            childColumns = ["pengembalian_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = ItemPengembalianEntity::class,
            parentColumns = ["id"],
            childColumns = ["item_pengembalian_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = ShiftEntity::class,
            parentColumns = ["id"],
            childColumns = ["shift_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("produk_id"),
        Index("transaksi_id"),
        Index("item_transaksi_id"),
        Index("pengembalian_id"),
        Index("item_pengembalian_id"),
        Index("shift_id"),
        Index(
            value = ["produk_id", "dibuat_pada", "id"]
        )
    ]
)
data class PergerakanPersediaanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "produk_id")
    val produkId: Long,
    val jenis: JenisPergerakanPersediaan,
    @ColumnInfo(name = "perubahan_jumlah")
    val perubahanJumlah: Long,
    @ColumnInfo(name = "perubahan_jumlah_rusak")
    val perubahanJumlahRusak: Long,
    @ColumnInfo(name = "saldo_jumlah_sebelum")
    val saldoJumlahSebelum: Long,
    @ColumnInfo(name = "saldo_jumlah_setelah")
    val saldoJumlahSetelah: Long,
    @ColumnInfo(name = "saldo_rusak_sebelum")
    val saldoRusakSebelum: Long,
    @ColumnInfo(name = "saldo_rusak_setelah")
    val saldoRusakSetelah: Long,
    @ColumnInfo(name = "transaksi_id")
    val transaksiId: Long?,
    @ColumnInfo(name = "item_transaksi_id")
    val itemTransaksiId: Long?,
    @ColumnInfo(name = "pengembalian_id")
    val pengembalianId: Long?,
    @ColumnInfo(name = "item_pengembalian_id")
    val itemPengembalianId: Long?,
    @ColumnInfo(name = "shift_id")
    val shiftId: Long?,
    val keterangan: String,
    @ColumnInfo(name = "dibuat_pada")
    val dibuatPada: Long,
)
