package com.sentral.org.data.converter

import androidx.room3.ColumnTypeConverter
import com.sentral.org.data.model.*

class DatabaseConverters {
    @ColumnTypeConverter fun statusKeranjangToString(v: StatusKeranjang?): String? = v?.name
    @ColumnTypeConverter fun stringToStatusKeranjang(v: String?): StatusKeranjang? = v?.let { enumValueOf<StatusKeranjang>(it) }

    @ColumnTypeConverter fun statusTransaksiToString(v: StatusTransaksi?): String? = v?.name
    @ColumnTypeConverter fun stringToStatusTransaksi(v: String?): StatusTransaksi? = v?.let { enumValueOf<StatusTransaksi>(it) }

    @ColumnTypeConverter fun statusShiftToString(v: StatusShift?): String? = v?.name
    @ColumnTypeConverter fun stringToStatusShift(v: String?): StatusShift? = v?.let { enumValueOf<StatusShift>(it) }

    @ColumnTypeConverter fun metodePembayaranToString(v: MetodePembayaran?): String? = v?.name
    @ColumnTypeConverter fun stringToMetodePembayaran(v: String?): MetodePembayaran? = v?.let { enumValueOf<MetodePembayaran>(it) }

    @ColumnTypeConverter fun jenisDiskonToString(v: JenisDiskon?): String? = v?.name
    @ColumnTypeConverter fun stringToJenisDiskon(v: String?): JenisDiskon? = v?.let { enumValueOf<JenisDiskon>(it) }

    @ColumnTypeConverter fun tujuanStokToString(v: TujuanStokPengembalian?): String? = v?.name
    @ColumnTypeConverter fun stringToTujuanStok(v: String?): TujuanStokPengembalian? = v?.let { enumValueOf<TujuanStokPengembalian>(it) }

    @ColumnTypeConverter fun jenisPersediaanToString(v: JenisPergerakanPersediaan?): String? = v?.name
    @ColumnTypeConverter fun stringToJenisPersediaan(v: String?): JenisPergerakanPersediaan? = v?.let { enumValueOf<JenisPergerakanPersediaan>(it) }

    @ColumnTypeConverter fun jenisKasToString(v: JenisPergerakanKas?): String? = v?.name
    @ColumnTypeConverter fun stringToJenisKas(v: String?): JenisPergerakanKas? = v?.let { enumValueOf<JenisPergerakanKas>(it) }
}