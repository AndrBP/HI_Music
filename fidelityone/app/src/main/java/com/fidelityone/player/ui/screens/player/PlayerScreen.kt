package com.fidelityone.player.ui.screens.player

import androidx.compose.animation.animateContentSizeAsStateOf
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fidelityone.player.core.AudioFormat
import com.fidelityone.player.core.TechFormatter
import com.fidelityone.player.domain.model.PlaybackState
import com.fidelityone.player.domain.model.RepeatMode
import com.fidelityone.player.ui.screens.CoverArt
import com.fidelityone.player.ui.theme.LocalFidelityTokens
import com.fidelityone.player.viewmodel.PlayerUiState
import kotlinx.coroutines.delay

/** Tela de reprodução em tela cheia — o coração visual do app. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    ui: PlayerUiState,
    onClose: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSeek: (Long) -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onFavorite: () -> Unit,
    onOpenQueue: () -> Unit,
    onSleepTimer: (Int) -> Unit
) {
    val pb = ui.playback
    val track = pb.current
    val t = LocalFidelityTokens.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(track?.album ?: "Player", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Filled.ExpandMore, "Fechar") } },
                actions = {
                    SleepTimerMenu(onSleepTimer, ui.sleepRemainingMs)
                    IconButton(onClick = onOpenQueue) { Icon(Icons.Filled.QueueMusic, "Fila") }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.background)))
                .padding(horizontal = t.spacing.gutter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))

            // Capa grande
            CoverArt(
                track?.externalArtUri,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f).padding(8.dp)
            )

            Spacer(Modifier.height(16.dp))
            Text(
                track?.title ?: "Nada tocando",
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            Text(
                track?.let { "${it.artist} — ${it.album}" } ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )

            // Infos técnicas ao vivo
            TechnicalStrip(pb, track?.format)

            Spacer(Modifier.height(12.dp))

            // Barra de progresso
            var dragging by remember { mutableStateOf(false) }
            var dragValue by remember { mutableStateOf(0f) }
            LaunchedEffect(pb.positionMs, dragging) {
                if (!dragging && pb.durationMs > 0 && pb.isPlaying) delay(500)
            }
            val progress = if (dragging) dragValue / 100f
            else if (pb.durationMs > 0) (pb.positionMs.toFloat() / pb.durationMs).coerceIn(0f, 1f) else 0f

            Slider(
                value = progress * 100,
                onValueChange = { dragging = true; dragValue = it },
                onValueChangeFinished = {
                    onSeek((dragValue / 100 * pb.durationMs).toLong()); dragging = false
                }
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(TechFormatter.duration(if (dragging) (dragValue / 100 * pb.durationMs).toLong() else pb.positionMs),
                    style = MaterialTheme.typography.labelSmall)
                Text(TechFormatter.duration(pb.durationMs), style = MaterialTheme.typography.labelSmall)
            }

            Spacer(Modifier.height(12.dp))

            // Controles principais
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onShuffle) {
                    Icon(Icons.Filled.Shuffle, "Aleatório",
                        tint = if (pb.shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                }
                IconButton(onClick = onPrev) {
                    Icon(Icons.Filled.SkipPrevious, "Anterior", tint = MaterialTheme.colorScheme.onSurface)
                }
                FilledIconButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        if (pb.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        "Reproduzir/Pausar", modifier = Modifier.size(36.dp)
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.Filled.SkipNext, "Próxima", tint = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = onRepeat) {
                    Icon(
                        when (pb.repeatMode) {
                            RepeatMode.ONE -> Icons.Filled.RepeatOne
                            else -> Icons.Filled.Repeat
                        },
                        "Repetir",
                        tint = if (pb.repeatMode != RepeatMode.NONE) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onFavorite, shape = t.shapes.card) {
                    Icon(
                        if (ui.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, null,
                        tint = if (ui.isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (ui.isFavorite) "Favorita" else "Favoritar")
                }
                OutlinedButton(onClick = onOpenQueue, shape = t.shapes.card) {
                    Icon(Icons.Filled.QueueMusic, null); Spacer(Modifier.width(6.dp)); Text("Fila")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** Linha com formato · sample rate · bit depth · duração · bitrate estimado. */
@Composable
private fun TechnicalStrip(pb: PlaybackState, format: AudioFormat?) {
    val track = pb.current ?: return
    val sr = pb.liveSampleRateHz ?: track.sampleRateHz
    val bd = pb.liveBitDepth ?: track.bitDepth
    val parts = buildList {
        format?.label?.let { add(it) }
        pb.liveCodec?.let { add(it.uppercase()) }
        sr?.let { add(TechFormatter.sampleRate(if (format?.dsd == true && it >= 44100) it else it)) }
        TechFormatter.bitDepth(bd, format).takeIf { it != "—" }?.let { add(it) }
        add(TechFormatter.duration(track.durationMs))
        TechFormatter.bitrateEstimate(track.bitrateEstimateBps, format)?.takeIf { it != "—" }?.let { add(it) }
        if (pb.dsdFallbackToPcm) add("DSD→PCM")
    }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.padding(top = 8.dp)
    ) {
        Text(
            parts.joinToString("  ·  "),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun SleepTimerMenu(onSet: (Int) -> Unit, remainingMs: Long) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Filled.Sleep, "Sleep timer",
                tint = if (remainingMs > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
        }
        DropdownMenu(open, { open = false }) {
            listOf(15, 30, 45, 60, 90).forEach { m ->
                DropdownMenuItem(text = { Text("$m minutos") }, onClick = { onSet(m); open = false })
            }
            Divider()
            DropdownMenuItem(text = { Text("Cancelar") }, onClick = { onSet(0); open = false })
        }
    }
}
