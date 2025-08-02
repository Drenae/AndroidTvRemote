package com.telecommande.ui.remote.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.telecommande.ui.remote.buttons.BackButton
import com.telecommande.ui.remote.buttons.HomeButton
import com.telecommande.ui.remote.buttons.KeyboardButton

@Composable
fun NavSection(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit,
    onKeyboardClick: () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BackButton(onClick = onBackClick)
        HomeButton(onClick = onHomeClick)
        KeyboardButton(onClick = onKeyboardClick)
    }
}