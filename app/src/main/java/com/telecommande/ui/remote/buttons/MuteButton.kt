package com.telecommande.ui.remote.buttons

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.telecommande.R
import com.telecommande.ui.remote.composables.RemoteButton

@Composable
fun MuteButton(
    onClick: () -> Unit
) {
    RemoteButton(
        onClick = onClick,
        iconRes = R.drawable.ic_mute,
        contentDescription = "Mute",
        modifier = Modifier,
    )
}