package com.sentral.org

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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
        setContent {
            PosNavHost()
        }
    }
}