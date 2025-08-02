package com.telecommande.ui.remote.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.telecommande.R
import com.telecommande.ui.remote.buttons.MuteButton
import com.telecommande.ui.remote.composables.RemoteVerticalButton

@Composable
fun MainControlsSection(
    modifier: Modifier = Modifier,
    onVolumeUpClick: () -> Unit,
    onVolumeDownClick: () -> Unit,
    onMuteClick: () -> Unit,
    onChannelUpClick: () -> Unit,
    onChannelDownClick: () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Center
    ) {
        RemoteVerticalButton(
            onUpClick = onVolumeUpClick,
            onDownClick = onVolumeDownClick,
            iconRes = R.drawable.ic_volume,
            contentDescription = "Volume",
            modifier = Modifier.padding(end = 8.dp)
        )

        Column(
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MuteButton(onClick = onMuteClick)
        }

        RemoteVerticalButton(
            onUpClick = onChannelUpClick,
            onDownClick = onChannelDownClick,
            iconRes = R.drawable.ic_channel,
            contentDescription = "Channel",
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

