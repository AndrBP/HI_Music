package com.fidelityone.player.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fidelityone.player.ui.theme.LocalFidelityTokens
import com.fidelityone.player.ui.theme.PlayerBarStyle

/**
 * MiniPlayer persistente (barra acima do conteúdo).
 * Estilo definido pelos tokens do layout ativo: SLIM / FLOATING / FULL_WIDTH_GLASS.
 */
@Composable
fun MiniPlayerBar(
    title: String?,
    artist: String?,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onOpen: () -> Unit
) {
    if (title == null) return
    val t = LocalFidelityTokens.current
    Surface(
        modifier = when (t.playerBar) {
            PlayerBarStyle.FLOATING -> Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
            else -> Modifier.fillMaxWidth()
        },
        shape = if (t.playerBar == PlayerBarStyle.FULL_WIDTH_GLASS)
            MaterialTheme.shapes.none else t.shapes.bar,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(
            alpha = if (t.playerBar == PlayerBarStyle.FULL_WIDTH_GLASS) 0.85f else 1f
        ),
        tonalElevation = 3.dp,
        shadowElevation = if (t.playerBar == PlayerBarStyle.FLOATING) 6.dp else 0.dp
    ) {
        Row(
            Modifier
                .height(if (t.playerBar == PlayerBarStyle.SLIM) 56.dp else 68.dp)
                .clickable(onClick = onOpen)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.GraphicEq, null,
                tint = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(artist ?: "", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary, maxLines = 1)
            }
            IconButton(onClick = onPlayPause) {
                Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, "Play/Pause")
            }
            IconButton(onClick = onNext) { Icon(Icons.Filled.SkipNext, "Próxima") }
        }
    }
}
