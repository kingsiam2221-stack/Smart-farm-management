package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class AppThemeMode(val prefValue: String) {
    DEFAULT("system"),
    WHITE("light"),
    DARK("dark");

    fun toOverride(): Boolean? = when (this) {
        DEFAULT -> null
        WHITE -> false
        DARK -> true
    }

    fun next(): AppThemeMode = when (this) {
        DEFAULT -> WHITE
        WHITE -> DARK
        DARK -> DEFAULT
    }

    companion object {
        fun fromOverride(override: Boolean?): AppThemeMode = when (override) {
            null -> DEFAULT
            false -> WHITE
            true -> DARK
        }
    }
}

private val DefaultLightColors = lightColorScheme(
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

private val WhiteModeColors = lightColorScheme(
    primary = ForestGreen,
    onPrimary = PureWhite,
    primaryContainer = Color(0xFFE4F4E7),
    onPrimaryContainer = ForestGreenDark,
    secondary = SkyTeal,
    onSecondary = PureWhite,
    secondaryContainer = Color(0xFFE3F5F2),
    onSecondaryContainer = ForestGreenDark,
    tertiary = HarvestGold,
    onTertiary = PureWhite,
    tertiaryContainer = HarvestGoldContainer,
    onTertiaryContainer = LightTextPrimary,
    background = PureWhite,
    onBackground = LightTextPrimary,
    surface = PureWhite,
    onSurface = LightTextPrimary,
    surfaceVariant = Color(0xFFF3F5F4),
    onSurfaceVariant = LightTextSecondary,
    outline = Color(0xFFD5E0D7),
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
    val DarkSecondary = Color(0xFF64D8CB)
    val DarkError = Color(0xFFFF8A80)
}

val SmartFarmShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp)
)

@Composable
fun SmartFarmTheme(
    themeMode: AppThemeMode = AppThemeMode.DEFAULT,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.WHITE -> false
        AppThemeMode.DEFAULT -> darkTheme
    }
    val colors = when {
        useDark -> DarkColors
        themeMode == AppThemeMode.WHITE -> WhiteModeColors
        else -> DefaultLightColors
    }
    MaterialTheme(
        colorScheme = colors,
        typography = SmartFarmTypography,
        shapes = SmartFarmShapes,
        content = content
    )
}
