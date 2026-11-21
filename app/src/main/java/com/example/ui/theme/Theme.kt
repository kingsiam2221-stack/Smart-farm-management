package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = ForestGreen,
    onPrimary = PureWhite,
    primaryContainer = MintContainer,
    onPrimaryContainer = ForestGreenDark,
    secondary = SkyTeal,
    onSecondary = PureWhite,
    secondaryContainer = SkyTealContainer,
    onSecondaryContainer = ForestGreenDark,
    tertiary = HarvestGold,
    onTertiary = PureWhite,
    tertiaryContainer = HarvestGoldContainer,
    onTertiaryContainer = LightTextPrimary,
    background = SoftSageSurface,
    onBackground = LightTextPrimary,
    surface = PureWhite,
    onSurface = LightTextPrimary,
    surfaceVariant = MintTint,
    onSurfaceVariant = LightTextSecondary,
    outline = LightOutline,
    error = BarnTerracotta,
    errorContainer = BarnTerracottaContainer,
    onError = PureWhite
)

private val DarkColors = darkColorScheme(
    primary = DarkEmeraldPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkMintContainer,
    onPrimaryContainer = DarkTextPrimary,
    secondary = ColorTokens.DarkSecondary,
    onSecondary = DarkOnPrimary,
    secondaryContainer = DarkForestSurfaceVariant,
    onSecondaryContainer = DarkTextPrimary,
    tertiary = HarvestGold,
    onTertiary = PureWhite,
    tertiaryContainer = DarkForestSurfaceVariant,
    onTertiaryContainer = HarvestGoldContainer,
    background = DarkForestBackground,
    onBackground = DarkTextPrimary,
    surface = DarkForestSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkForestSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutline,
    error = ColorTokens.DarkError,
    errorContainer = BarnTerracottaContainer,
    onError = DarkTextPrimary
)

private object ColorTokens {
    val DarkSecondary = androidx.compose.ui.graphics.Color(0xFF64D8CB)
    val DarkError = androidx.compose.ui.graphics.Color(0xFFFF8A80)
}

val SmartFarmShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp)
)

@Composable
fun SmartFarmTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = SmartFarmTypography,
        shapes = SmartFarmShapes,
        content = content
    )
}
