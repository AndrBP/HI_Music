package com.fidelityone.player.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fidelityone.player.domain.model.AppSettings

/** Configurações: áudio (gapless/crossfade/ReplayGain), tema atalho, equalizador atalho. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onCrossfade: (Int) -> Unit,
    onReplayGain: (Boolean) -> Unit,
    onReplayGainAlbum: (Boolean) -> Unit,
    onGapless: (Boolean) -> Unit,
    onOpenThemes: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onRescan: () -> Unit
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Configurações") }) }) { pad ->
        Column(
            Modifier
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Aparência
            Text("Aparência", style = MaterialTheme.typography.titleMedium)
            ListItem(
                headlineContent = { Text("Temas e layouts") },
                supportingContent = { Text("${settings.preset.label} · 20 combinações disponíveis") },
                leadingContent = { Icon(Icons.Filled.Palette, null) },
                trailingContent = { Button(onClick = onOpenThemes) { Text("Escolher") } }
            )
            HorizontalDivider()

            // Áudio
            Text("Qualidade de áudio", style = MaterialTheme.typography.titleMedium)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Reprodução sem silêncio (gapless)", style = MaterialTheme.typography.bodyMedium)
                    Text("Nativo do decoder; mantém transições contínuas entre faixas do álbum.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary)
                }
                Switch(checked = settings.gapless, onCheckedChange = onGapless)
            }

            CrossfadeSlider(settings.crossfadeMs, onCrossfade)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("ReplayGain (normalização por tag)", style = MaterialTheme.typography.bodyMedium)
                    Text("Aplica ganho quando o arquivo possui tags ReplayGain. Não altera os dados originais.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary)
                }
                Switch(checked = settings.replayGainEnabled, onCheckedChange = onReplayGain)
            }
            if (settings.replayGainEnabled) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Usar ganho de álbum (em vez de faixa)", Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = settings.replayGainSourceAlbum, onCheckedChange = onReplayGainAlbum)
                }
            }

            HorizontalDivider()

            // Equalizador
            ListItem(
                headlineContent = { Text("Equalizador de 10 bandas") },
                supportingContent = { Text("Presets, preamp, bypass e presets personalizados") },
                leadingContent = { Icon(Icons.Filled.Tune, null) },
                trailingContent = { Button(onClick = onOpenEqualizer) { Text("Abrir") } }
            )
            HorizontalDivider()

            // Biblioteca
            OutlinedButton(onClick = onRescan, modifier = Modifier.fillMaxWidth()) {
                Text("Sincronizar biblioteca novamente")
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "Fidelity One v0.1.0-mvp · local-first · funciona offline.\n" +
                        "Permissões: somente leitura de arquivos de áudio.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun CrossfadeSlider(ms: Int, onChange: (Int) -> Unit) {
    Column {
        Text("Crossfade: ${if (ms == 0) "desligado" else "${ms / 1000}s"}",
            style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = ms.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = 0f..12_000f,
            steps = 11
        )
    }
}
