package com.sentral.org.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.sentral.org.data.DatabaseWarmup
import com.sentral.org.data.session.SesiKasirProvider
import com.sentral.org.ui.navigation.PosRoute
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.sqlite.SQLiteException
import java.io.IOException

class MainViewModel(
    private val warmup: DatabaseWarmup,
    private val sessionProvider: SesiKasirProvider,
) : ViewModel() {

    private companion object {
        private val log = Logger.withTag("MainViewModel")
    }

    private val _isReady = MutableStateFlow(false)
    val isReady = _isReady.asStateFlow()

    private val _startDestination = MutableStateFlow<PosRoute>(PosRoute.LoginKasir)
    val startDestination = _startDestination.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                warmup.warm()
                val sesi = sessionProvider.sesiAktif()
                _startDestination.value = if (sesi != null) { PosRoute.PosUtama } else { PosRoute.LoginKasir }
            } catch (e: CancellationException) {
                throw e
            } catch (e: androidx.sqlite.SQLiteException) {
                log.e(e) { "Warm-up database gagal: ${e.message}" }
            } catch (e: IOException) {
                log.e(e) { "Warm-up storage gagal: ${e.message}" }
            } finally {
                _isReady.value = true
            }
        }
    }
}