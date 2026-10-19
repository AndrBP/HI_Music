package com.fidelityone.player.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.fidelityone.player.domain.model.ColorTheme
import com.fidelityone.player.domain.model.VisualPreset

/* Paletas premium originais (sem copiar identidade de terceiros). */

private val OnyxDark = darkColorScheme(
    primary = Color(0xFFE8E4DC),          // prata quente
    onPrimary = Color(0xFF17161A),
    secondary = Color(0xFF9AA3B2),
    tertiary = Color(0xFFC9A86A),         // detalhe âmbar discreto
    background = Color(0xFF0B0B0F),
    surface = Color(0xFF141419),
    surfaceVariant = Color(0xFF1D1D24),
    onBackground = Color(0xFFE6E4E8),
    onSurface = Color(0xFFE6E4E8),
    outline = Color(0xFF3A3A44)
)

private val IvoryLight = lightColorScheme(
    primary = Color(0xFF2A2A2E),
    onPrimary = Color(0xFFFAF8F4),
    secondary = Color(0xFF6B6F76),
    tertiary = Color(0xFF8A6D3B),
    background = Color(0xFFFAF8F4),       // marfim
    surface = Color(0xFFF2EFE8),
    surfaceVariant = Color(0xFFEAE6DD),
    onBackground = Color(0xFF1C1B1E),
    onSurface = Color(0xFF1C1B1E),
    outline = Color(0xFFC9C4B8)
)

private val MidnightBlue = darkColorScheme(
    primary = Color(0xFF9FB8E8),
    onPrimary = Color(0xFF0E1626),
    secondary = Color(0xFF7E8CA8),
    tertiary = Color(0xFFE0C580),
    background = Color(0xFF0A1122),
    surface = Color(0xFF101A30),
    surfaceVariant = Color(0xFF17233D),
    onBackground = Color(0xFFE2E8F5),
    onSurface = Color(0xFFE2E8F5),
    outline = Color(0xFF2E3C58)
)

private val GraphiteGold = darkColorScheme(
    primary = Color(0xFFD9B45B),          // grafite + ouro
    onPrimary = Color(0xFF1F1B12),
    secondary = Color(0xFFA89A7B),
    tertiary = Color(0xFFEDE4CC),
    background = Color(0xFF121212),
    surface = Color(0xFF1A1917),
    surfaceVariant = Color(0xFF23221E),
    onBackground = Color(0xFFEDEAE2),
    onSurface = Color(0xFFEDEAE2),
    outline = Color(0xFF454033)
)

@Composable
fun FidelityOneTheme(
    preset: VisualPreset,
    content: @Composable () -> Unit
) {
    val dark = preset.theme != ColorTheme.IVORY_LIGHT
    val scheme = when (preset.theme) {
        ColorTheme.ONYX_DARK -> OnyxDark
        ColorTheme.IVORY_LIGHT -> IvoryLight
        ColorTheme.MIDNIGHT_BLUE -> MidnightBlue
        ColorTheme.GRAPHITE_GOLD -> GraphiteGold
    }
    val tokens = LayoutTokens.forLayout(preset.layout.id)

    CompositionLocalProvider(LocalFidelityTokens provides tokens) {
        MaterialTheme(
            colorScheme = scheme,
            typography = typographyFrom(tokens.type),
            shapes = MaterialTheme.shapes,
            content = content
        )
    }
}

/** Gradiente de fundo por layout (usado nas telas principais). */
@Composable
fun backgroundColors(): List<Color> {
    val tokens = LocalFidelityTokens.current
    val s = MaterialTheme.colorScheme
    return when (tokens.background) {
        BackgroundStyle.SOLID -> listOf(s.background, s.background)
        BackgroundStyle.GRADIENT_DEEP -> listOf(s.surface, s.background, Color.Black)
        BackgroundStyle.MUTED_WASH -> listOf(s.surfaceVariant, s.background)
    }
}
