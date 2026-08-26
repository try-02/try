package com.sentral.org.data

import androidx.room3.ColumnTypeConverters
import androidx.room3.Database
import androidx.room3.RoomDatabase
import com.sentral.org.data.converter.DatabaseConverters
import com.sentral.org.data.dao.*
import com.sentral.org.data.entity.*

@Database(
    entities = [
        ProdukEntity::class,
        PersediaanEntity::class,
        PergerakanPersediaanEntity::class,
        KasirEntity::class,
        ShiftEntity::class,
        PergerakanKasEntity::class,
        KeranjangEntity::class,
        ItemKeranjangEntity::class,
        TransaksiEntity::class,
        ItemTransaksiEntity::class,
        PembayaranEntity::class,
        PengembalianEntity::class,
        ItemPengembalianEntity::class,
        PrinterEntity::class,
        ProfilTokoEntity::class,
    ],
    version = 1,
    exportSchema = true
)
@ColumnTypeConverters(DatabaseConverters::class)
abstract class PosDatabase : RoomDatabase() {
    abstract fun produkDao(): ProdukDao
    abstract fun persediaanDao(): PersediaanDao
    abstract fun pergerakanPersediaanDao(): PergerakanPersediaanDao
    abstract fun kasirDao(): KasirDao
    abstract fun shiftDao(): ShiftDao
    abstract fun pergerakanKasDao(): PergerakanKasDao
    abstract fun keranjangDao(): KeranjangDao
    abstract fun itemKeranjangDao(): ItemKeranjangDao
    abstract fun transaksiDao(): TransaksiDao
    abstract fun itemTransaksiDao(): ItemTransaksiDao
    abstract fun pembayaranDao(): PembayaranDao
    abstract fun returDao(): ReturDao
    abstract fun printerDao(): PrinterDao
    abstract fun profilTokoDao(): ProfilTokoDao
}