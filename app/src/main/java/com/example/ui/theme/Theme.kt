package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val CommercialStudioColorScheme = darkColorScheme(
    primary = GoldPrimary,
    onPrimary = ObsidianBlack,
    primaryContainer = SoftGoldContainer,
    onPrimaryContainer = ChampagneGold,
    secondary = ClaudeTerracotta,
    onSecondary = ObsidianBlack,
    secondaryContainer = CardSurfaceHigh,
    onSecondaryContainer = WarmIvory,
    tertiary = DeepAmberGold,
    onTertiary = WarmIvory,
    background = ObsidianBlack,
    onBackground = WarmIvory,
    surface = VelvetCharcoal,
    onSurface = WarmIvory,
    surfaceVariant = ElevatedObsidian,
    onSurfaceVariant = MutedWarmGray,
    surfaceContainerLowest = ObsidianBlack,
    surfaceContainerLow = VelvetCharcoal,
    surfaceContainer = ElevatedObsidian,
    surfaceContainerHigh = CardSurfaceHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
    outline = GoldBorderStrong,
    outlineVariant = GraphiteDivider,
    error = CrimsonError,
    onError = ObsidianBlack,
    errorContainer = CrimsonContainer,
    onErrorContainer = CrimsonError
)

private val ArchitecturalShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(18.dp)
)

@Composable
fun ClaudeClassicTheme(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = CommercialStudioColorScheme,
            typography = ClaudeTypography,
            shapes = ArchitecturalShapes,
            content = content
        )
    }
}
