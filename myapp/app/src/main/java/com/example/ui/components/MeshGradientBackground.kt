package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.ui.theme.*

@Composable
fun MeshGradientBackground(
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    if (!enabled) {
        Canvas(modifier = modifier.fillMaxSize().background(BgBase)) {}
        return
    }

    Canvas(modifier = modifier.fillMaxSize().background(BgBase)) {
        val width = size.width
        val height = size.height

        drawMeshBlob(
            color = MeshBlob1,
            center = Offset(width * 0.1f, height * 0.04f),
            radius = width * 1.2f,
            alpha = 0.50f
        )

        drawMeshBlob(
            color = MeshBlob2,
            center = Offset(width * 0.95f, height * 0.22f),
            radius = width * 1.1f,
            alpha = 0.40f
        )

        drawMeshBlob(
            color = MeshBlob3,
            center = Offset(width * 0.08f, height * 0.55f),
            radius = width * 1.3f,
            alpha = 0.34f
        )

        drawMeshBlob(
            color = MeshBlob4,
            center = Offset(width * 0.9f, height * 0.72f),
            radius = width * 1.2f,
            alpha = 0.36f
        )

        drawMeshBlob(
            color = MeshBlob5,
            center = Offset(width * 0.3f, height * 0.96f),
            radius = width * 1.1f,
            alpha = 0.32f
        )

        drawMeshBlob(
            color = MeshBlob6,
            center = Offset(width * 0.88f, height * 0.06f),
            radius = width * 0.8f,
            alpha = 0.08f
        )
    }
}

private fun DrawScope.drawMeshBlob(
    color: Color,
    center: Offset,
    radius: Float,
    alpha: Float
) {
    if (radius <= 0f) return
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = alpha), color.copy(alpha = 0f)),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}
