package com.telecommande.ui.remote.buttons

import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.telecommande.R
import com.telecommande.ui.remote.composables.RemoteButton

@Composable
fun KeyboardButton(
    onClick: () -> Unit
) {
    RemoteButton(
        onClick = onClick,
        iconRes = R.drawable.ic_keyboard,
        contentDescription = "Clavier",
        modifier = Modifier.offset(y = (-32).dp)
    )
}