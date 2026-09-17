package com.sentral.org.ui

import co.touchlab.kermit.Logger
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentral.org.data.DatabaseWarmup
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(private val warmup: DatabaseWarmup) : ViewModel() {

    private companion object {
        val log = Logger.withTag("MainVM")
    }

    private val _isReady = MutableStateFlow(false)
    val isReady = _isReady.asStateFlow()

    init {
        viewModelScope.launch {
            _isReady.update { try {
                warmup.warm()
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Gagal membuka DB tetap dilepas ke UI agar tidak splash selamanya;
                // error operasional akan muncul pada alur fitur masing-masing.
                log.e(e) { "Warm-up database gagal" }
                true
            }
        }
    }
}