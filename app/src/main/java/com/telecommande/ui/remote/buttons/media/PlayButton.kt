package com.telecommande.ui.remote.buttons.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.telecommande.R
import com.telecommande.ui.remote.composables.RemoteButton

@Composable
fun PlayButton(
    onClick: () -> Unit
) {
    RemoteButton(
        onClick = onClick,
        iconRes = R.drawable.ic_play,
        contentDescription = "Play",
        modifier = Modifier
    )
}