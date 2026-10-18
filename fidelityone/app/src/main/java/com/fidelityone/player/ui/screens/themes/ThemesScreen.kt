package com.fidelityone.player.ui.screens.themes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fidelityone.player.domain.model.ColorTheme
import com.fidelityone.player.domain.model.LayoutBase
import com.fidelityone.player.domain.model.VisualPreset
import com.fidelityone.player.ui.theme.PresetCatalog

/**
 * Tela de Temas: os 20 presets (5 layouts × 4 cores) + seleção avulsa
 * de layout e cor para combinações livres.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemesScreen(
    current: VisualPreset,
    onPreset: (VisualPreset) -> Unit,
    onClose: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Temas · ${PresetCatalog.all.size} presets") },
                navigationIcon = { TextButton(onClick = onClose) { Text("Fechar") } }
            )
        }
    ) { pad ->
        Column(Modifier.padding(pad)) {
            Text(
                "Combinações visuais: escolha um preset pronto ou componha layout + cor.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                contentPadding = PaddingValues(12.dp, 0.dp, 12.dp, 24.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(PresetCatalog.all, key = { it.id }) { preset ->
                    PresetTile(
                        preset = preset,
                        selected = preset == current,
                        onClick = { onPreset(preset) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetTile(preset: VisualPreset, selected: Boolean, onClick: () -> Unit) {
    val bg = themeBackground(preset.theme)
    val accent = themeAccent(preset.theme)
    Card(
        modifier = Modifier
            .padding(6.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        border = if (selected) BorderStroke(2.dp, accent) else null
    ) {
        Column {
            // mini-preview do layout
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(Brush.verticalGradient(bg))
                    .padding(10.dp)
            ) {
                when (preset.layout) {
                    LayoutBase.CLASSIC_LIBRARY -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(2) { MiniBox(accent, Modifier.size(34.dp).clip(MaterialTheme.shapes.medium)) }
                    }
                    LayoutBase.MINIMAL_PLAYER -> Column(verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically), modifier = Modifier.fillMaxSize()) {
                        repeat(3) { MiniBox(accent.copy(alpha = 0.5f), Modifier.fillMaxWidth().height(10.dp)) }
                    }
                    LayoutBase.AUDIOPHILE_DASHBOARD -> Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                        repeat(3) { MiniBox(accent, Modifier.size(24.dp).clip(MaterialTheme.shapes.small)) }
                    }
                    LayoutBase.EDITORIAL_PREMIUM -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        MiniBox(accent, Modifier.fillMaxWidth(0.7f).height(14.dp).clip(MaterialTheme.shapes.extraSmall))
                        MiniBox(accent.copy(alpha = 0.4f), Modifier.fillMaxWidth(0.9f).height(8.dp))
                    }
                    LayoutBase.COMPACT_HIFI -> Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(4) { MiniBox(accent, Modifier.size(16.dp).clip(MaterialTheme.shapes.small)) }
                    }
                }
                if (selected) {
                    Icon(
                        Icons.Filled.Check, "Selecionado",
                        tint = accent,
                        modifier = Modifier.align(Alignment.TopEnd).size(18.dp)
                    )
                }
            }
            Text(
                preset.label,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

private fun MiniBox(color: Color, modifier: Modifier) = Box(modifier.background(color))

@Composable
private fun themeBackground(theme: ColorTheme): List<Color> = when (theme) {
    ColorTheme.ONYX_DARK -> listOf(Color(0xFF141419), Color(0xFF0B0B0F))
    ColorTheme.IVORY_LIGHT -> listOf(Color(0xFFFAF8F4), Color(0xFFEAE6DD))
    ColorTheme.MIDNIGHT_BLUE -> listOf(Color(0xFF101A30), Color(0xFF0A1122))
    ColorTheme.GRAPHITE_GOLD -> listOf(Color(0xFF1A1917), Color(0xFF121212))
}

@Composable
private fun themeAccent(theme: ColorTheme): Color = when (theme) {
    ColorTheme.ONYX_DARK -> Color(0xFFC9A86A)
    ColorTheme.IVORY_LIGHT -> Color(0xFF2A2A2E)
    ColorTheme.MIDNIGHT_BLUE -> Color(0xFF9FB8E8)
    ColorTheme.GRAPHITE_GOLD -> Color(0xFFD9B45B)
}
