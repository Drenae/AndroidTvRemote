package com.telecommande.ui.remote.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.telecommande.R
import com.telecommande.ui.theme.GradientEnd
import com.telecommande.ui.theme.GradientStart

@Composable
fun RemoteVerticalButton(
    modifier: Modifier = Modifier,
    onUpClick: () -> Unit,
    onDownClick: () -> Unit,
    iconRes: Int,
    shape: Shape = RoundedCornerShape(36.dp),
    contentDescription: String,
    borderWidth: Dp = 1.dp,
    borderColor: Color = Color(0xFF161616)
) {
    Column(
        modifier = modifier
            .width(88.dp)
            .height(248.dp)
            .shadowCompat(8.dp, RoundedCornerShape(36.dp))
            .background(brush = Brush.linearGradient(colors = listOf(GradientStart, GradientEnd)), shape)
            .border(width = borderWidth, color = borderColor, shape = shape)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        RemoteButton(
            onClick = onUpClick,
            iconRes = R.drawable.ic_plus,
            contentDescription = "Increase $contentDescription",
            size = 74.dp
        )
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = contentDescription,
            modifier = Modifier.size(38.dp)
        )
        RemoteButton(
            onClick = onDownClick,
            iconRes = R.drawable.ic_minus,
            contentDescription = "Decrease $contentDescription",
            size = 74.dp
        )
    }
}