package com.telecommande.ui.remote.composables

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

@Composable
fun Modifier.shadowCompat(elevation: Dp, shape: androidx.compose.ui.graphics.Shape): Modifier {
    val elevationPx = with(LocalDensity.current) { elevation.toPx() }
    return this.then(
        Modifier.graphicsLayer(
            shadowElevation = elevationPx,
            shape = shape,
            clip = false
        )
    )
}