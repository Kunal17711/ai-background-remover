package com.bykunalbuilds.backgroundremover.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.bykunalbuilds.backgroundremover.R

private val LightColors = lightColorScheme(
    primary = Color(0xFF0C6B39),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD0F6DC),
    onPrimaryContainer = Color(0xFF08361E),
    secondary = Color(0xFF45534A),
    background = Color(0xFFF7F8F4),
    onBackground = Color(0xFF171B18),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF171B18),
    surfaceVariant = Color(0xFFE8ECE7),
    onSurfaceVariant = Color(0xFF58615B),
    outline = Color(0xFF737C76),
    outlineVariant = Color(0xFFD2D8D3),
    error = Color(0xFFB3261E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5FE28F),
    onPrimary = Color(0xFF06321C),
    primaryContainer = Color(0xFF174E2D),
    onPrimaryContainer = Color(0xFFBAF4CD),
    secondary = Color(0xFFBBC9BF),
    background = Color(0xFF101311),
    onBackground = Color(0xFFE8ECE8),
    surface = Color(0xFF181C19),
    onSurface = Color(0xFFE8ECE8),
    surfaceVariant = Color(0xFF252B27),
    onSurfaceVariant = Color(0xFFB8C1BA),
    outline = Color(0xFF89928B),
    outlineVariant = Color(0xFF353C37),
    error = Color(0xFFFFB4AB),
)

@Composable
fun BackgroundRemoverTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
    ) {
        ProvideTextStyle(value = AppTypography.bodyMedium, content = content)
    }
}

private val Manrope = FontFamily(
    Font(R.font.manrope_variable, FontWeight.Normal),
    Font(R.font.manrope_variable, FontWeight.Medium),
    Font(R.font.manrope_variable, FontWeight.SemiBold),
    Font(R.font.manrope_variable, FontWeight.Bold),
    Font(R.font.manrope_variable, FontWeight.ExtraBold),
)

private val Defaults = Typography()

private val AppTypography = Typography(
    displayLarge = Defaults.displayLarge.copy(
        fontFamily = Manrope,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 48.sp,
        lineHeight = 52.sp,
        letterSpacing = (-1.5).sp,
    ),
    displayMedium = Defaults.displayMedium.copy(fontFamily = Manrope, fontWeight = FontWeight.Bold),
    displaySmall = Defaults.displaySmall.copy(fontFamily = Manrope, fontWeight = FontWeight.Bold),
    headlineLarge = Defaults.headlineLarge.copy(fontFamily = Manrope, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineSmall = Defaults.headlineSmall.copy(fontFamily = Manrope, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleMedium = Defaults.titleMedium.copy(fontFamily = Manrope, fontWeight = FontWeight.SemiBold),
    titleSmall = Defaults.titleSmall.copy(fontFamily = Manrope, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = Defaults.bodyMedium.copy(fontFamily = Manrope, fontWeight = FontWeight.Normal),
    bodySmall = Defaults.bodySmall.copy(fontFamily = Manrope, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = Defaults.labelMedium.copy(fontFamily = Manrope, fontWeight = FontWeight.SemiBold),
    labelSmall = Defaults.labelSmall.copy(fontFamily = Manrope, fontWeight = FontWeight.Bold),
)

val ColorScheme.checkerLight: Color get() = if (background.luminance() > .5f) Color(0xFFF6F7F5) else Color(0xFF202522)
val ColorScheme.checkerDark: Color get() = if (background.luminance() > .5f) Color(0xFFE2E6E2) else Color(0xFF151916)
