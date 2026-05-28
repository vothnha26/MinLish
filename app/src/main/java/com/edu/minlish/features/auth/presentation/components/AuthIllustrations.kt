package com.edu.minlish.features.auth.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun GoogleIconDrawing() {
    Canvas(modifier = Modifier.size(18.dp)) {
        val w = size.width
        val h = size.height
        val strokeW = 3.dp.toPx()
        
        // Top Red
        drawArc(
            color = Color(0xFFEA4335),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = true
        )
        // Right Blue
        drawArc(
            color = Color(0xFF4285F4),
            startAngle = 270f,
            sweepAngle = 180f,
            useCenter = true
        )
        // Bottom Green
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = true
        )
        // Left Yellow
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 90f,
            sweepAngle = 180f,
            useCenter = true
        )
        // Draw center white circle to make it look like a ring / donut
        drawCircle(
            color = Color.White,
            radius = size.minDimension / 3
        )
        // Draw horizontal line for "G" center
        drawLine(
            color = Color(0xFF4285F4),
            start = Offset(w / 2, h / 2),
            end = Offset(w * 0.9f, h / 2),
            strokeWidth = 2.dp.toPx()
        )
    }
}

@Composable
fun LockIconDrawing() {
    Canvas(modifier = Modifier.size(40.dp)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 2.dp.toPx()

        // Base box
        drawRoundRect(
            color = Color(0xFF111111),
            topLeft = Offset(w * 0.2f, h * 0.45f),
            size = Size(w * 0.6f, h * 0.45f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
            style = Stroke(width = strokeWidth)
        )

        // Shackle arch
        val shacklePath = Path().apply {
            moveTo(w * 0.325f, h * 0.45f)
            lineTo(w * 0.325f, h * 0.325f)
            cubicTo(w * 0.325f, h * 0.125f, w * 0.675f, h * 0.125f, w * 0.675f, h * 0.325f)
            lineTo(w * 0.675f, h * 0.45f)
        }
        drawPath(
            path = shacklePath,
            color = Color(0xFF111111),
            style = Stroke(width = strokeWidth)
        )

        // Keyhole
        drawCircle(
            color = Color(0xFF111111),
            radius = 2.5.dp.toPx(),
            center = Offset(w / 2f, h * 0.65f)
        )
        drawLine(
            color = Color(0xFF111111),
            start = Offset(w / 2f, h * 0.7f),
            end = Offset(w / 2f, h * 0.8f),
            strokeWidth = strokeWidth
        )
    }
}

@Composable
fun EnvelopeIconDrawing() {
    Canvas(modifier = Modifier.size(40.dp)) {
        val w = size.width
        val h = size.height
        val strokeWidth = 2.dp.toPx()

        // Mail box
        drawRoundRect(
            color = Color(0xFF111111),
            topLeft = Offset(w * 0.1f, h * 0.25f),
            size = Size(w * 0.8f, h * 0.55f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
            style = Stroke(width = strokeWidth)
        )

        // V flap lines
        val flapPath = Path().apply {
            moveTo(w * 0.1f, h * 0.35f)
            lineTo(w * 0.5f, h * 0.625f)
            lineTo(w * 0.9f, h * 0.35f)
        }
        drawPath(
            path = flapPath,
            color = Color(0xFF111111),
            style = Stroke(width = strokeWidth)
        )

        // Badge at top right
        drawCircle(
            color = Color.White,
            radius = 7.dp.toPx(),
            center = Offset(w * 0.825f, h * 0.25f)
        )
        drawCircle(
            color = Color(0xFF111111),
            radius = 7.dp.toPx(),
            center = Offset(w * 0.825f, h * 0.25f),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Check inside badge
        val checkPath = Path().apply {
            moveTo(w * 0.77f, h * 0.25f)
            lineTo(w * 0.81f, h * 0.29f)
            lineTo(w * 0.88f, h * 0.21f)
        }
        drawPath(
            path = checkPath,
            color = Color(0xFF111111),
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}
