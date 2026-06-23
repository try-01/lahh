package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderStrong
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.GlassSurfacePressed

@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 18.dp,
    customGradient: Brush? = null,
    customBorderColor: Color? = null,
    testTag: String? = null,
    onPressUpdate: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = tween(90),
        label = "glass_button_scale"
    )

    if (isPressed) {
        LaunchedEffect(Unit) {
            onPressUpdate?.invoke()
        }
    }

    val shape = RoundedCornerShape(cornerRadius)

    val backgroundModifier = when {
        customGradient != null -> Modifier.background(customGradient, shape)
        isPressed -> Modifier.background(GlassSurfacePressed, shape)
        else -> Modifier.background(GlassSurface, shape)
    }

    val finalBorderColor = when {
        customBorderColor != null -> customBorderColor
        isPressed -> GlassBorderStrong
        else -> GlassBorder
    }

    Box(
        modifier = modifier
            .scale(scale)
            .then(backgroundModifier)
            .border(1.dp, finalBorderColor, shape)
            .clip(shape)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
