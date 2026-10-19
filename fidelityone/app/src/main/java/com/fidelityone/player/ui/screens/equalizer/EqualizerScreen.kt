package com.fidelityone.player.ui.screens.equalizer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fidelityone.player.domain.model.EqPreset
import com.fidelityone.player.viewmodel.EqualizerUiState
import kotlin.math.roundToInt

/** Equalizador profissional: 10 bandas gráficas + preamp + presets + bypass/reset/salvar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    ui: EqualizerUiState,
    onSelectPreset: (EqPreset) -> Unit,
    onBandGain: (Int, Float) -> Unit,
    onPreamp: (Float) -> Unit,
    onBypass: () -> Unit,
    onReset: () -> Unit,
    onSaveCustom: (String) -> Unit,
    onApplyCustom: (String, FloatArray) -> Unit,
    onClose: () -> Unit
) {
    val state = ui.state
    var saveName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Equalizador") },
                navigationIcon = { TextButton(onClick = onClose) { Text("Fechar") } },
                actions = {
                    IconButton(onClick = onBypass) {
                        Icon(Icons.Filled.Power, "Bypass",
                            tint = if (state.enabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline)
                    }
                    IconButton(onClick = onReset) { Icon(Icons.Filled.Refresh, "Reset") }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!ui.supported) {
                AssistChip(onClick = {}, label = {
                    Text("EQ de 10 bandas requer Android 9+. Neste aparelho o EQ fica inativo.")
                })
            }

            // Presets
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EqPreset.entries.filter { it != EqPreset.CUSTOM }.forEach { p ->
                    FilterChip(
                        selected = state.preset == p,
                        onClick = { onSelectPreset(p) },
                        label = { Text(p.label) }
                    )
                }
                ui.customPresets.forEach { (name, gains) ->
                    FilterChip(
                        selected = state.customName == name,
                        onClick = { onApplyCustom(name, gains) },
                        label = { Text(name) }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Preamp
            Text("Preamp: ${state.preampDb.roundToInt()} dB", style = MaterialTheme.typography.titleMedium)
            Slider(
                value = state.preampDb,
                onValueChange = onPreamp,
                valueRange = -15f..15f,
                steps = 29
            )

            Spacer(Modifier.height(16.dp))

            // 10 bandas — sliders verticais lado a lado
            Row(
                Modifier.fillMaxWidth().height(300.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                state.bands.forEachIndexed { i, band ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(44.dp)
                    ) {
                        Text(
                            "${band.gainDb.roundToInt()}dB",
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center
                        )
                        Box(Modifier.weight(1f).padding(vertical = 4.dp)) {
                            // Slider vertical via rotation simples
                            Slider(
                                value = band.gainDb,
                                onValueChange = { onBandGain(i, it) },
                                valueRange = -15f..15f,
                                modifier = Modifier
                                    .size(height = 180.dp, width = 32.dp)
                                    .graphicsLayer { rotationZ = 270f },
                                reverseLayout = true
                            )
                        }
                        Text(
                            freqLabel(band.frequencyHz),
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Salvar preset custom
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = saveName,
                    onValueChange = { saveName = it },
                    placeholder = { Text("Nome do preset") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { if (saveName.isNotBlank()) { onSaveCustom(saveName); saveName = "" } },
                    shape = MaterialTheme.shapes.large
                ) {
                    Icon(Icons.Filled.Save, null); Spacer(Modifier.width(6.dp)); Text("Salvar")
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "Arquitetura pronta para DSP próprio (paramétrico completo com Q por banda) em versão futura.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

private fun freqLabel(hz: Float): String = if (hz >= 1000) "${(hz / 1000).roundToInt()}k" else hz.roundToInt().toString()
