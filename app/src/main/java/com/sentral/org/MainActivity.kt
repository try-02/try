package com.sentral.org

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sentral.org.ui.MainViewModel
import com.sentral.org.ui.navigation.PosNavHost
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    // Koin menyediakan MainViewModel beserta DatabaseWarmup-nya.
    private val mainViewModel: MainViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        // Tahan splash sampai koneksi database benar-benar terbuka.
        splashScreen.setKeepOnScreenCondition { !mainViewModel.isReady.value }
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge agar konten bisa mengisi area system bars
        enableEdgeToEdge()
        
        setContent {
            val isReady by mainViewModel.isReady.collectAsStateWithLifecycle()
            val startDestination by mainViewModel.startDestination.collectAsStateWithLifecycle()

            if (isReady) {
                PosNavHost(startDestination = startDestination)
            }
        }
    }
}