package com.telecommande.ui.remote.buttons.dpad

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.telecommande.R
import com.telecommande.ui.remote.composables.RemoteDpadButton

@Composable
fun LeftButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    RemoteDpadButton(
        onClick = onClick,
        iconRes = R.drawable.ic_dpad_left,
        contentDescription = "Left",
        modifier = modifier
    )
}

