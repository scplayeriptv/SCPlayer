package com.sc.player.ui.player

import com.sc.player.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import com.sc.player.ui.theme.Cyan
import com.sc.player.ui.theme.TextMuted
import java.util.Locale

private class TrackItem(val group: Tracks.Group, val index: Int, val label: String, val selected: Boolean)

private fun label(f: Format, position: Int, ctx: android.content.Context): String {
    val lang = f.language?.takeIf { it != "und" }?.let { Locale.forLanguageTag(it).displayLanguage }?.takeIf { it.isNotBlank() }
    val base = f.label?.takeIf { it.isNotBlank() } ?: lang ?: ctx.getString(R.string.player_track, position)
    val ch = if (f.channelCount > 0 && f.sampleMimeType?.startsWith("audio") == true) {
        when (f.channelCount) { 1 -> " · Mono"; 2 -> " · Stereo"; 6 -> " · 5.1"; 8 -> " · 7.1"; else -> " · ${f.channelCount}ch" }
    } else ""
    return base + ch
}

private fun items(tracks: Tracks, type: Int, ctx: android.content.Context): List<TrackItem> {
    val out = ArrayList<TrackItem>()
    var n = 1
    for (g in tracks.groups) {
        if (g.type != type) continue
        for (i in 0 until g.length) {
            if (!g.isTrackSupported(i)) continue
            out += TrackItem(g, i, label(g.getTrackFormat(i), n++, ctx), g.isTrackSelected(i))
        }
    }
    return out
}

/** نافذة خيارات المشغّل: الصوت، الترجمة، نسبة الصورة. */
@Composable
internal fun PlayerOptionsDialog(
    player: Player,
    tracks: Tracks,
    aspect: Int,
    onAspect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val audio = items(tracks, C.TRACK_TYPE_AUDIO, ctx)
    val text = items(tracks, C.TRACK_TYPE_TEXT, ctx)
    val textOff = text.none { it.selected }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.player_options)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (audio.size > 1) {
                    Header(stringResource(R.string.player_audio))
                    audio.forEach { a ->
                        Choice(a.label, a.selected) {
                            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                                .setOverrideForType(TrackSelectionOverride(a.group.mediaTrackGroup, a.index))
                                .build()
                        }
                    }
                }
                if (text.isNotEmpty()) {
                    Header(stringResource(R.string.player_subtitles))
                    Choice(stringResource(R.string.player_off), textOff) {
                        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                            .build()
                    }
                    text.forEach { s ->
                        Choice(s.label, s.selected) {
                            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                .setOverrideForType(TrackSelectionOverride(s.group.mediaTrackGroup, s.index))
                                .build()
                        }
                    }
                }
                Header(stringResource(R.string.player_aspect))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(R.string.aspect_fit, R.string.aspect_stretch, R.string.aspect_zoom).forEachIndexed { i, res ->
                        Choice(stringResource(res), aspect == i, Modifier.weight(1f)) { onAspect(i) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) } },
    )
}

@Composable
private fun Header(text: String) {
    Text(text, color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 10.dp))
}

@Composable
private fun Choice(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = modifier.then(if (modifier == Modifier) Modifier.fillMaxWidth() else Modifier)) {
        Text(
            (if (selected) "✓ " else "") + text,
            color = if (selected) Cyan else androidx.compose.ui.graphics.Color.Unspecified,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = if (modifier == Modifier) Modifier.fillMaxWidth() else Modifier,
        )
    }
}
