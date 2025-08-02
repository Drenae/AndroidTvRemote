package com.telecommande.ui.remote.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.telecommande.ui.remote.buttons.media.PlayButton
import com.telecommande.ui.remote.buttons.media.RewindBackButton
import com.telecommande.ui.remote.buttons.media.RewindForwardButton
import com.telecommande.ui.remote.buttons.media.StopButton

@Composable
fun MediaSection(
    modifier: Modifier = Modifier,
    onRewindBackClick: () -> Unit,
    onPlayClick: () -> Unit,
    onStopClick: () -> Unit,
    onRewindForwardClick: () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        RewindBackButton(onClick = onRewindBackClick)
        PlayButton(onClick = onPlayClick)
        StopButton(onClick = onStopClick)
        RewindForwardButton(onClick = onRewindForwardClick)
    }
}

