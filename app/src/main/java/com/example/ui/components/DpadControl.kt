package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentNav
import com.example.ui.theme.AccentNav2
import com.example.ui.theme.TextPrimary

@Composable
fun DpadControl(
    onUp: () -> Unit,
    onDown: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onOk: () -> Unit,
    onVibrate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(216.dp)
            .background(com.example.ui.theme.GlassSurface, CircleShape)
            .border(1.dp, com.example.ui.theme.GlassBorder, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        // Upper arrow
        DpadArrowButton(
            icon = Icons.Default.KeyboardArrowUp,
            contentDescription = "Up",
            onClick = onUp,
            onVibrate = onVibrate,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
                .testTag("dpad_up")
        )

        // Lower arrow
        DpadArrowButton(
            icon = Icons.Default.KeyboardArrowDown,
            contentDescription = "Down",
            onClick = onDown,
            onVibrate = onVibrate,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
                .testTag("dpad_down")
        )

        // Left arrow
        DpadArrowButton(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = "Left",
            onClick = onLeft,
            onVibrate = onVibrate,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp)
                .testTag("dpad_left")
        )

        // Right arrow
        DpadArrowButton(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Right",
            onClick = onRight,
            onVibrate = onVibrate,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
                .testTag("dpad_right")
        )

        // Center OK button - polished solid lavender
        GlassButton(
            onClick = onOk,
            modifier = Modifier
                .size(74.dp)
                .testTag("dpad_ok"),
            cornerRadius = 37.dp, // circular
            customGradient = Brush.linearGradient(
                listOf(
                    com.example.ui.theme.AccentNav,
                    com.example.ui.theme.AccentNav
                )
            ),
            customBorderColor = Color.Transparent,
            onPressUpdate = onVibrate
        ) {
            Text(
                text = "OK",
                color = com.example.ui.theme.AccentNav2, // Dark contrast text color (#381E72)
                fontSize = 15.sp, // beautiful bold text
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.03.sp
            )
        }
    }
}

@Composable
fun DpadArrowButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    onVibrate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = tween(90),
        label = "arrow_pressed_scale"
    )

    if (isPressed) {
        LaunchedEffect(Unit) {
            onVibrate()
        }
    }

    Box(
        modifier = modifier
            .size(50.dp)
            .scale(scale)
            .clip(RoundedCornerShape(15.dp))
            .background(if (isPressed) com.example.ui.theme.GlassSurfacePressed else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = TextPrimary.copy(alpha = 0.85f),
            modifier = Modifier.size(24.dp)
        )
    }
}
