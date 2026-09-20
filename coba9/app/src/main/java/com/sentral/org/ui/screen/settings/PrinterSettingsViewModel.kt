package com.sentral.org.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentral.org.data.entity.PrinterEntity
import com.sentral.org.data.entity.ProfilTokoEntity
import com.sentral.org.data.repository.PrinterRepository
import com.sentral.org.data.repository.ProfilTokoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PrinterItemUi(
    val id: Long,
    val nama: String,
    val tipeKoneksi: String,
    val karakterPerBaris: Int,
    val isDefault: Boolean,
    val dinonaktifkanOtomatis: Boolean,
    val gagalStatusBerturut: Int,
)

fun PrinterEntity.toUi() = PrinterItemUi(
    id = id,
    nama = nama,
    tipeKoneksi = tipeKoneksi,
    karakterPerBaris = karakterPerBaris,
    isDefault = isDefault,
    dinonaktifkanOtomatis = dinonaktifkanOtomatis,
    gagalStatusBerturut = gagalStatusBerturut,
)

data class PrinterSettingsUiState(
    val printers: List<PrinterItemUi> = emptyList(),
    val cetakOtomatis: Boolean = true,
)

class PrinterSettingsViewModel(
    private val printerRepo: PrinterRepository,
    private val profilRepo: ProfilTokoRepository,
) : ViewModel() {

    val uiState: StateFlow<PrinterSettingsUiState> = kotlinx.coroutines.flow.combine(
        printerRepo.observeAll().map { list -> list.map { it.toUi() } },
        profilRepo.observe(),
    ) { printers, profil ->
        PrinterSettingsUiState(
            printers = printers,
            cetakOtomatis = profil?.cetakOtomatis ?: true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PrinterSettingsUiState())

    fun toggleCetakOtomatis(aktif: Boolean) {
        viewModelScope.launch {
            val current = profilRepo.get() ?: ProfilTokoEntity(
                id = 1,
                namaToko = "Toko POS",
                alamat = "",
                catatanFooter = "",
                logoUri = null,
                cetakOtomatis = aktif,
            )
            profilRepo.save(current.copy(cetakOtomatis = aktif))
        }
    }
}