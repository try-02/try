package com.sentral.org.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ============================================================
// DESIGN SYSTEM — Material 3 Expressive "CobaApp Signature"
// Berani, modern, dengan ciri khas yang kuat tapi tetap profesional.
// ============================================================

// ---------- Brand Colors ----------
private val PurplePrimary = Color(0xFF6D28D9)        // Deep Purple - Modern & Tech
private val PurpleLight = Color(0xFFA78BFA)          // Light Purple
private val PurpleDark = Color(0xFF4C1D95)           // Dark Purple

private val LimeSecondary = Color(0xFF84CC16)        // Fresh Lime - Energetic
private val LimeLight = Color(0xFFBEF264)
private val LimeDark = Color(0xFF4D7C0F)

private val CoralTertiary = Color(0xFFF43F5E)        // Coral Pink - Bold CTA
private val CoralLight = Color(0xFFFB7185)
private val CoralDark = Color(0xFF9F1239)

// ---------- Light Theme ----------
private val LightBackground = Color(0xFFFAFAF9)      // Warm off-white
private val LightSurface = Color(0xFFFFFFFF)
private val LightSurfaceVariant = Color(0xFFF5F5F4)
private val LightOnSurface = Color(0xFF1C1917)       // Warm black
private val LightOnSurfaceVariant = Color(0xFF78716C)
private val LightOutline = Color(0xFFD6D3D1)
private val LightOutlineVariant = Color(0xFFE7E5E4)

// ---------- Dark Theme ----------
private val DarkBackground = Color(0xFF0F0F0F)       // Deep black
private val DarkSurface = Color(0xFF1C1917)          // Warm dark gray
private val DarkSurfaceVariant = Color(0xFF292524)
private val DarkOnSurface = Color(0xFFFAFAF9)
private val DarkOnSurfaceVariant = Color(0xFFA8A29E)
private val DarkOutline = Color(0xFF57534E)
private val DarkOutlineVariant = Color(0xFF44403C)

private val LightColors = lightColorScheme().copy(
    primary = PurplePrimary,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = PurpleDark,
    secondary = LimeSecondary,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFECFCCB),
    onSecondaryContainer = LimeDark,
    tertiary = CoralTertiary,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFE4E6),
    onTertiaryContainer = CoralDark,
    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B),
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
)

private val DarkColors = darkColorScheme().copy(
    primary = PurpleLight,
    onPrimary = PurpleDark,
    primaryContainer = PurplePrimary,
    onPrimaryContainer = Color(0xFFEDE9FE),
    secondary = LimeLight,
    onSecondary = LimeDark,
    secondaryContainer = LimeSecondary,
    onSecondaryContainer = Color(0xFFECFCCB),
    tertiary = CoralLight,
    onTertiary = CoralDark,
    tertiaryContainer = CoralTertiary,
    onTertiaryContainer = Color(0xFFFFE4E6),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF991B1B),
    onErrorContainer = Color(0xFFFEE2E2),
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
)

/**
 * Expressive Shapes — Lebih berani dengan radius besar dan squircle feel.
 * Menciptakan kesan modern dan playful tapi tetap profesional.
 */
private val PosShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

private val BaseTypography = Typography()

private val PosTypography = BaseTypography.copy(
    displayLarge = BaseTypography.displayLarge.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        letterSpacing = (-0.5).sp,
    ),
    displayMedium = BaseTypography.displayMedium.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.25).sp,
    ),
    displaySmall = BaseTypography.displaySmall.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
    ),
    headlineLarge = BaseTypography.headlineLarge.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.25).sp,
    ),
    headlineMedium = BaseTypography.headlineMedium.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
    ),
    headlineSmall = BaseTypography.headlineSmall.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
    ),
    titleLarge = BaseTypography.titleLarge.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
    ),
    titleMedium = BaseTypography.titleMedium.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.15.sp,
    ),
    titleSmall = BaseTypography.titleSmall.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.1.sp,
    ),
    bodyLarge = BaseTypography.bodyLarge.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
    ),
    bodyMedium = BaseTypography.bodyMedium.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
    ),
    bodySmall = BaseTypography.bodySmall.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
    ),
    labelLarge = BaseTypography.labelLarge.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = BaseTypography.labelMedium.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = BaseTypography.labelSmall.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.5.sp,
    ),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PosTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialExpressiveTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = PosTypography,
        shapes = PosShapes,
        content = content,
    )
}
