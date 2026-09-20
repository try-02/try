package com.sentral.org.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sentral.org.data.DatabaseWarmup
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(private val warmup: DatabaseWarmup) : ViewModel() {

    private val _isReady = MutableStateFlow(false)
    val isReady = _isReady.asStateFlow()

    init {
        viewModelScope.launch {
            _isReady.value = try {
                warmup.warm()
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Gagal membuka DB tetap dilepas ke UI agar tidak splash selamanya;
                // error operasional akan muncul pada alur fitur masing-masing.
                Log.e("MainViewModel", "Warm-up database gagal", e)
                true
            }
        }
    }
}