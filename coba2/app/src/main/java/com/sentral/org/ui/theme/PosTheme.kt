package com.sentral.org.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ---------- Palet warna: indigo modern dengan aksen amber ----------

// Light
private val IndigoPrimary = Color(0xFF4F46E5)
private val IndigoOnPrimary = Color(0xFFFFFFFF)
private val IndigoPrimaryContainer = Color(0xFFE0E7FF)
private val IndigoOnPrimaryContainer = Color(0xFF1E1B4B)
private val SecondaryTeal = Color(0xFF0D9488)
private val AmberAccent = Color(0xFFF59E0B)
private val LightBackground = Color(0xFFF4F5FB)
private val LightSurface = Color(0xFFFFFFFF)
private val LightSurfaceVariant = Color(0xFFEAECF5)

// Dark
private val DarkPrimary = Color(0xFFA5B4FC)
private val DarkOnPrimary = Color(0xFF1E1B4B)
private val DarkPrimaryContainer = Color(0xFF3730A3)
private val DarkOnPrimaryContainer = Color(0xFFE0E7FF)
private val DarkBackground = Color(0xFF101122)
private val DarkSurface = Color(0xFF181A2E)
private val DarkSurfaceVariant = Color(0xFF232644)

private val LightColors = lightColorScheme(
    primary = IndigoPrimary,
    onPrimary = IndigoOnPrimary,
    primaryContainer = IndigoPrimaryContainer,
    onPrimaryContainer = IndigoOnPrimaryContainer,
    secondary = SecondaryTeal,
    tertiary = AmberAccent,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    error = Color(0xFFDC2626),
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = Color(0xFF5EEAD4),
    tertiary = Color(0xFFFBBF24),
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    error = Color(0xFFF87171),
)

// Bentuk sudut lebih halus untuk kesan modern
private val PosShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Tema aplikasi POS. Mengikuti dark mode sistem secara otomatis. */
@Composable
fun PosTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = PosShapes,
        content = content,
    )
}
