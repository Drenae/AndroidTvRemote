package com.telecommande.ui.remote.buttons

import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.telecommande.R
import com.telecommande.ui.remote.composables.RemoteButton

@Composable
fun BackButton(
    onClick: () -> Unit
) {
    RemoteButton(
        onClick = onClick,
        iconRes = R.drawable.ic_back,
        contentDescription = "Retour",
        modifier = Modifier.offset(y = (-32).dp)
    )
}