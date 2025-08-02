package com.telecommande.ui.remote.buttons.dpad

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.telecommande.R
import com.telecommande.ui.remote.composables.RemoteButton

@Composable
fun OkButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    RemoteButton(
        onClick = onClick,
        iconRes = R.drawable.ic_dpad_ok,
        contentDescription = "OK",
        size = 120.dp,
        modifier = modifier
    )
}