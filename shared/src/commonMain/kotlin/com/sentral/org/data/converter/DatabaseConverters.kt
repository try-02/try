package com.sentral.org.data.converter

import androidx.room3.ColumnTypeConverter
import com.sentral.org.data.model.*

/**
 * Helper parsing enum yang aman: kalau string tidak cocok dengan enum value,
 * kembalikan null (bukan crash). Penting untuk ketahanan aplikasi saat DB
 * corrupt atau ada data usang dari migrasi manual.
 */
private inline fun <reified T : Enum<T>> safeEnumValueOf(name: String?): T? =
    name?.let { target -> enumValues<T>().firstOrNull { it.name == target } }

class DatabaseConverters {
    @ColumnTypeConverter fun statusKeranjangToString(v: StatusKeranjang?): String? = v?.name
    @ColumnTypeConverter fun stringToStatusKeranjang(v: String?): StatusKeranjang? = safeEnumValueOf(v)

    @ColumnTypeConverter fun statusTransaksiToString(v: StatusTransaksi?): String? = v?.name
    @ColumnTypeConverter fun stringToStatusTransaksi(v: String?): StatusTransaksi? = safeEnumValueOf(v)

    @ColumnTypeConverter fun statusShiftToString(v: StatusShift?): String? = v?.name
    @ColumnTypeConverter fun stringToStatusShift(v: String?): StatusShift? = safeEnumValueOf(v)

    @ColumnTypeConverter fun metodePembayaranToString(v: MetodePembayaran?): String? = v?.name
    @ColumnTypeConverter fun stringToMetodePembayaran(v: String?): MetodePembayaran? = safeEnumValueOf(v)

    @ColumnTypeConverter fun jenisDiskonToString(v: JenisDiskon?): String? = v?.name
    @ColumnTypeConverter fun stringToJenisDiskon(v: String?): JenisDiskon? = safeEnumValueOf(v)

    @ColumnTypeConverter fun tujuanStokToString(v: TujuanStokPengembalian?): String? = v?.name
    @ColumnTypeConverter fun stringToTujuanStok(v: String?): TujuanStokPengembalian? = safeEnumValueOf(v)

    @ColumnTypeConverter fun jenisPersediaanToString(v: JenisPergerakanPersediaan?): String? = v?.name
    @ColumnTypeConverter fun stringToJenisPersediaan(v: String?): JenisPergerakanPersediaan? = safeEnumValueOf(v)

    @ColumnTypeConverter fun jenisKasToString(v: JenisPergerakanKas?): String? = v?.name
    @ColumnTypeConverter fun stringToJenisKas(v: String?): JenisPergerakanKas? = safeEnumValueOf(v)
}