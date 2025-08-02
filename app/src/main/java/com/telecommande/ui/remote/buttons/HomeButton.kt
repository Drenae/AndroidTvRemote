package com.telecommande.ui.remote.buttons

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.telecommande.R
import com.telecommande.ui.remote.composables.RemoteButton

@Composable
fun HomeButton(
    onClick: () -> Unit
) {
    RemoteButton(
        onClick = onClick,
        iconRes = R.drawable.ic_home,
        contentDescription = "Accueil",
        modifier = Modifier
    )
}