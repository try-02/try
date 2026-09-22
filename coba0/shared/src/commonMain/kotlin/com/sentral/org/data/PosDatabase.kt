package com.sentral.org.data

import androidx.room3.ColumnTypeConverters
import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import com.sentral.org.data.converter.DatabaseConverters
import com.sentral.org.data.dao.ProdukDao
import com.sentral.org.data.dao.PersediaanDao
import com.sentral.org.data.dao.PergerakanPersediaanDao
import com.sentral.org.data.dao.KasirDao
import com.sentral.org.data.dao.ShiftDao
import com.sentral.org.data.dao.PergerakanKasDao
import com.sentral.org.data.dao.KeranjangDao
import com.sentral.org.data.dao.ItemKeranjangDao
import com.sentral.org.data.dao.TransaksiDao
import com.sentral.org.data.dao.ItemTransaksiDao
import com.sentral.org.data.dao.PembayaranDao
import com.sentral.org.data.dao.ReturDao
import com.sentral.org.data.dao.PrinterDao
import com.sentral.org.data.dao.ProfilTokoDao
import com.sentral.org.data.entity.ProdukEntity
import com.sentral.org.data.entity.PersediaanEntity
import com.sentral.org.data.entity.PergerakanPersediaanEntity
import com.sentral.org.data.entity.KasirEntity
import com.sentral.org.data.entity.ShiftEntity
import com.sentral.org.data.entity.PergerakanKasEntity
import com.sentral.org.data.entity.KeranjangEntity
import com.sentral.org.data.entity.ItemKeranjangEntity
import com.sentral.org.data.entity.TransaksiEntity
import com.sentral.org.data.entity.ItemTransaksiEntity
import com.sentral.org.data.entity.PembayaranEntity
import com.sentral.org.data.entity.PengembalianEntity
import com.sentral.org.data.entity.ItemPengembalianEntity
import com.sentral.org.data.entity.PrinterEntity
import com.sentral.org.data.entity.ProfilTokoEntity

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
    exportSchema = true,
)
@ColumnTypeConverters(DatabaseConverters::class)
@ConstructedBy(PosDatabaseConstructor::class)
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

// Room 3.0 + KMP: @ConstructedBy menghubungkan abstract database ke generated
// constructor. `expect object` ini WAJIB di commonMain; Room/KSP yang generate
// `actual` implementasinya (jangan buat `actual object` manual — itu yg bikin
// error "@ConstructedBy definition must be an 'expect' declaration").
@Suppress("KotlinNoActualForExpect", "NO_ACTUAL_FOR_EXPECT")
expect object PosDatabaseConstructor : RoomDatabaseConstructor<PosDatabase> {
    override fun initialize(): PosDatabase
}
