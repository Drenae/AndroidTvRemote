package com.telecommande.ui.remote.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintLayout
import com.telecommande.ui.remote.buttons.dpad.DownButton
import com.telecommande.ui.remote.buttons.dpad.LeftButton
import com.telecommande.ui.remote.buttons.dpad.OkButton
import com.telecommande.ui.remote.buttons.dpad.RightButton
import com.telecommande.ui.remote.buttons.dpad.UpButton

@Composable
fun DpadSection(
    modifier: Modifier = Modifier,
    onOkClick: () -> Unit,
    onUpClick: () -> Unit,
    onDownClick: () -> Unit,
    onLeftClick: () -> Unit,
    onRightClick: () -> Unit,
    borderWidth: Dp = 1.dp,
    borderColor: Color = Color(0xFF161616),
    shape: Shape = CircleShape,
) {
    ConstraintLayout(
        modifier = modifier
            .size(280.dp)
            .background(brush = Brush.linearGradient(colors = listOf(Color(0xFF2C2C2E), Color(0xFF161616))), shape = CircleShape)
            .border(width = borderWidth, color = borderColor, shape = shape)
            .padding(0.dp)
    ) {
        val (okBtn, upBtn, downBtn, leftBtn, rightBtn) = createRefs()

        OkButton(
            onClick = onOkClick,
            modifier = Modifier
                .constrainAs(okBtn) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        )

        UpButton(
            onClick = onUpClick,
            modifier = Modifier.constrainAs(upBtn) {
                top.linkTo(parent.top, margin = 1.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        )
        
        DownButton(
            onClick = onDownClick,
            modifier = Modifier.constrainAs(downBtn) {
                bottom.linkTo(parent.bottom, margin = 1.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        )

        LeftButton(
            onClick = onLeftClick,
            modifier = Modifier.constrainAs(leftBtn) {
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                start.linkTo(parent.start, margin = 1.dp)
            }
        )

        RightButton(
            onClick = onRightClick,
            modifier = Modifier.constrainAs(rightBtn) {
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                end.linkTo(parent.end, margin = 1.dp)
            }
        )
    }
}
