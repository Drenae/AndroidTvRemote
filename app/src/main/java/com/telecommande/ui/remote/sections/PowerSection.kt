package com.telecommande.ui.remote.sections

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.telecommande.R
import com.telecommande.ui.remote.buttons.PowerButton

@Composable
fun PowerSection(
    modifier: Modifier = Modifier,
    onPowerClick: () -> Unit,
    isConnected: Boolean,
    onStatusIndicatorClick: () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        PowerButton(onClick = onPowerClick)
        StatusIndicator(
            isConnected = isConnected,
            onClick = onStatusIndicatorClick,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
fun StatusIndicator(
    isConnected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    connectedIconRes: Int = R.drawable.ic_status_on,
    disconnectedIconRes: Int = R.drawable.ic_status_off
) {
    val interactionSource = remember { MutableInteractionSource() }
    Icon(
        painter = painterResource(id = if (isConnected) connectedIconRes else disconnectedIconRes),
        contentDescription = if (isConnected) "Status: Connecté - Gérer les TVs" else "Status: Déconnecté - Gérer les TVs",
        modifier = modifier
            .size(44.dp)
            .clickable(
                onClick = onClick,
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button
            ),
        tint = Color.Unspecified
    )
}