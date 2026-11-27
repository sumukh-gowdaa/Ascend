package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class AscendExtendedColors(
    val cardBorder: Color = MidnightBorder,
    val cardBorderStrong: Color = MidnightBorderStrong,
    val surfaceElevated: Color = MidnightSurfaceElevated,
    val surfaceHighlight: Color = MidnightSurfaceHighlight,
    val deepPurple: Color = DeepPurple,
    val textMuted: Color = DuskText,
    val doFirst: Color = QuadrantDoFirst,
    val doFirstBg: Color = QuadrantDoFirstBg,
    val schedule: Color = QuadrantSchedule,
    val scheduleBg: Color = QuadrantScheduleBg,
    val delegate: Color = QuadrantDelegate,
    val delegateBg: Color = QuadrantDelegateBg,
    val eliminate: Color = QuadrantEliminate,
    val eliminateBg: Color = QuadrantEliminateBg,
    val mint: Color = MintGreen,
    val mintBg: Color = MintGreenBg,
    val gold: Color = WarmGold,
    val goldBg: Color = WarmGoldBg,
    val softRed: Color = SoftRed,
    val softRedBg: Color = SoftRedBg,
    val skyBlue: Color = SkyBlue,
    val skyBlueBg: Color = SkyBlueBg,
    val ghostViolet: Color = SpectralViolet,
    val ghostRose: Color = SpectralRose,
    val ghostBg: Color = SpectralBg
)

val LocalAscendColors = staticCompositionLocalOf { AscendExtendedColors() }

private val AscendDarkColorScheme = darkColorScheme(
    primary = LavenderPrimary,
    onPrimary = LavenderOnPrimary,
    primaryContainer = LavenderContainer,
    onPrimaryContainer = LavenderLight,
    secondary = MintGreen,
    onSecondary = Color(0xFF0E231A),
    secondaryContainer = MintGreenBg,
    onSecondaryContainer = MintGreen,
    tertiary = SkyBlue,
    onTertiary = Color(0xFF0D202B),
    tertiaryContainer = SkyBlueBg,
    onTertiaryContainer = SkyBlue,
    background = MidnightBackground,
    onBackground = StarlightText,
    surface = MidnightSurface,
    onSurface = StarlightText,
    surfaceVariant = MidnightSurfaceElevated,
    onSurfaceVariant = MoonstoneText,
    surfaceContainerLowest = MidnightBackground,
    surfaceContainerLow = MidnightSurface,
    surfaceContainer = MidnightSurfaceElevated,
    surfaceContainerHigh = MidnightSurfaceHighlight,
    outline = MidnightBorderStrong,
    outlineVariant = MidnightBorder,
    error = ErrorCrimson,
    onError = Color(0xFF2E0D14),
    errorContainer = ErrorContainerDark,
    onErrorContainer = Color(0xFFFFDCE1)
)

private val AscendShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun AscendTheme(
    content: @Composable () -> Unit
) {
    val extendedColors = AscendExtendedColors()
    CompositionLocalProvider(LocalAscendColors provides extendedColors) {
        MaterialTheme(
            colorScheme = AscendDarkColorScheme,
            typography = AscendTypography,
            shapes = AscendShapes,
            content = content
        )
    }
}

object AscendThemeTokens {
    val colors: AscendExtendedColors
        @Composable get() = LocalAscendColors.current
}
