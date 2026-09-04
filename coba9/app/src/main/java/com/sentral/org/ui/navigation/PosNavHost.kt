package com.sentral.org.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sentral.org.ui.screen.pos.PosUtamaScreen
import com.sentral.org.ui.theme.PosTheme

import com.sentral.org.ui.screen.settings.PrinterSettingsScreen
import com.sentral.org.ui.screen.settings.AddPrinterScreen
import com.sentral.org.data.service.PrinterService
// import androidx.compose.runtime.rememberCoroutineScope
// import kotlinx.coroutines.launch
// import org.koin.compose.koinInject // Koin untuk Compose Multiplatform

@Composable
fun PosNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    // Rute awal kini langsung menuju POS Utama, karena Splash Screen 
    // sudah ditangani sepenuhnya oleh level Activity
    startDestination: PosRoute = PosRoute.PosUtama
) {
    PosTheme {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = modifier
        ) {

            // Rute LoginKasir bisa kamu siapkan nanti
            composable<PosRoute.LoginKasir> {
                // LoginScreen(...)
            }

            composable<PosRoute.PosUtama> {
                PosUtamaScreen(
                    onNavigateToRiwayat = { 
                        navController.navigate(PosRoute.RiwayatTransaksi()) 
                    },
                    onNavigateToTutupShift = {
                        // TODO: Logika navigasi tutup shift
                    },
                    onNavigateToPrinterSettings = {
                        navController.navigate(PosRoute.PrinterSettings)
                    },
                )
            }

            composable<PosRoute.PrinterSettings> {
                PrinterSettingsScreen(
                    onBack = { navController.popBackStack() },
                    onAddPrinter = { 
                        navController.navigate(PosRoute.AddPrinter) // <-- Hubungkan ke route AddPrinter
                    },
                )
            }

            composable<PosRoute.AddPrinter> {
                AddPrinterScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { 
                        // Tidak perlu reload di sini, sudah dilakukan di ViewModel
                        navController.popBackStack()
                    },
                )
            }

            composable<PosRoute.RiwayatTransaksi> {
                // RiwayatScreen(...)
            }
        }
    }
}
