package com.telecommande.ui.remote.buttons

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.telecommande.R
import com.telecommande.ui.remote.composables.RemoteButton

@Composable
fun PowerButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    RemoteButton(
        onClick = onClick,
        iconRes = R.drawable.ic_power,
        contentDescription = "Power",
        modifier = modifier
    )
}

