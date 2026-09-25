package com.inweb.browser.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Small source-drawn glyph set for the renderer draft.
 *
 * The pinned Chromium tree has Compose Material 3 but no Material icon target
 * (HOME-PAGE-HANDOFF A10). These glyphs keep the draft inside available
 * Compose UI graphics and the A9 new-Kotlin-files-only carve-out. The Lead
 * replaces product/utility artwork with governed drawables after the resource
 * injection path is proven.
 */
internal enum class HomeGlyphKind {
    BRAND,
    PRIVACY,
    MENU,
    SEARCH,
    VOICE,
    ADD,
    RECENT,
    BOOKMARK,
    DOWNLOAD,
    PLAY,
    RETRY,
    SHARE,
    FORWARD,
}

@Composable
internal fun HomeGlyph(
    kind: HomeGlyphKind,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    size: Dp = 24.dp,
) {
    val semanticsModifier = if (contentDescription == null) {
        modifier.clearAndSetSemantics { }
    } else {
        modifier.semantics { this.contentDescription = contentDescription }
    }
    Canvas(modifier = semanticsModifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = (minOf(w, h) * 0.085f).coerceAtLeast(1f)
        val line = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)

        when (kind) {
            HomeGlyphKind.BRAND -> {
                drawCircle(tint, radius = minOf(w, h) * 0.39f, style = line)
                drawLine(tint, Offset(w * 0.5f, h * 0.18f), Offset(w * 0.5f, h * 0.82f), stroke, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.18f, h * 0.5f), Offset(w * 0.82f, h * 0.5f), stroke, StrokeCap.Round)
            }
            HomeGlyphKind.PRIVACY -> {
                val path = Path().apply {
                    moveTo(w * 0.5f, h * 0.12f)
                    lineTo(w * 0.82f, h * 0.25f)
                    lineTo(w * 0.77f, h * 0.62f)
                    quadraticBezierTo(w * 0.72f, h * 0.82f, w * 0.5f, h * 0.9f)
                    quadraticBezierTo(w * 0.28f, h * 0.82f, w * 0.23f, h * 0.62f)
                    lineTo(w * 0.18f, h * 0.25f)
                    close()
                }
                drawPath(path, tint, style = line)
                drawLine(tint, Offset(w * 0.36f, h * 0.51f), Offset(w * 0.47f, h * 0.63f), stroke, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.47f, h * 0.63f), Offset(w * 0.68f, h * 0.39f), stroke, StrokeCap.Round)
            }
            HomeGlyphKind.MENU -> {
                for (y in listOf(0.28f, 0.5f, 0.72f)) {
                    drawLine(tint, Offset(w * 0.2f, h * y), Offset(w * 0.8f, h * y), stroke, StrokeCap.Round)
                }
            }
            HomeGlyphKind.SEARCH -> {
                drawCircle(tint, radius = minOf(w, h) * 0.27f, center = Offset(w * 0.43f, h * 0.43f), style = line)
                drawLine(tint, Offset(w * 0.63f, h * 0.63f), Offset(w * 0.84f, h * 0.84f), stroke, StrokeCap.Round)
            }
            HomeGlyphKind.VOICE -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.38f, h * 0.12f),
                    size = Size(w * 0.24f, h * 0.48f),
                    cornerRadius = CornerRadius(w * 0.12f, w * 0.12f),
                    style = line,
                )
                drawArc(tint, 0f, 180f, false, Offset(w * 0.24f, h * 0.35f), Size(w * 0.52f, h * 0.38f), style = line)
                drawLine(tint, Offset(w * 0.5f, h * 0.73f), Offset(w * 0.5f, h * 0.88f), stroke, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.37f, h * 0.88f), Offset(w * 0.63f, h * 0.88f), stroke, StrokeCap.Round)
            }
            HomeGlyphKind.ADD -> {
                drawLine(tint, Offset(w * 0.2f, h * 0.5f), Offset(w * 0.8f, h * 0.5f), stroke, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.5f, h * 0.2f), Offset(w * 0.5f, h * 0.8f), stroke, StrokeCap.Round)
            }
            HomeGlyphKind.RECENT -> {
                drawCircle(tint, radius = minOf(w, h) * 0.36f, style = line)
                drawLine(tint, Offset(w * 0.5f, h * 0.5f), Offset(w * 0.5f, h * 0.3f), stroke, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.5f, h * 0.5f), Offset(w * 0.67f, h * 0.59f), stroke, StrokeCap.Round)
            }
            HomeGlyphKind.BOOKMARK -> {
                val path = Path().apply {
                    moveTo(w * 0.28f, h * 0.14f)
                    lineTo(w * 0.72f, h * 0.14f)
                    lineTo(w * 0.72f, h * 0.86f)
                    lineTo(w * 0.5f, h * 0.7f)
                    lineTo(w * 0.28f, h * 0.86f)
                    close()
                }
                drawPath(path, tint, style = line)
            }
            HomeGlyphKind.DOWNLOAD -> {
                drawLine(tint, Offset(w * 0.5f, h * 0.15f), Offset(w * 0.5f, h * 0.62f), stroke, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.32f, h * 0.47f), Offset(w * 0.5f, h * 0.65f), stroke, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.68f, h * 0.47f), Offset(w * 0.5f, h * 0.65f), stroke, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.2f, h * 0.84f), Offset(w * 0.8f, h * 0.84f), stroke, StrokeCap.Round)
            }
            HomeGlyphKind.PLAY -> {
                val path = Path().apply {
                    moveTo(w * 0.32f, h * 0.18f)
                    lineTo(w * 0.8f, h * 0.5f)
                    lineTo(w * 0.32f, h * 0.82f)
                    close()
                }
                drawPath(path, tint)
            }
            HomeGlyphKind.RETRY -> {
                drawArc(tint, 35f, 285f, false, Offset(w * 0.16f, h * 0.16f), Size(w * 0.68f, h * 0.68f), style = line)
                drawLine(tint, Offset(w * 0.69f, h * 0.17f), Offset(w * 0.84f, h * 0.18f), stroke, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.84f, h * 0.18f), Offset(w * 0.8f, h * 0.34f), stroke, StrokeCap.Round)
            }
            HomeGlyphKind.SHARE -> {
                val points = listOf(
                    Offset(w * 0.26f, h * 0.5f),
                    Offset(w * 0.72f, h * 0.25f),
                    Offset(w * 0.72f, h * 0.75f),
                )
                drawLine(tint, points[0], points[1], stroke, StrokeCap.Round)
                drawLine(tint, points[0], points[2], stroke, StrokeCap.Round)
                points.forEach { drawCircle(tint, radius = w * 0.09f, center = it) }
            }
            HomeGlyphKind.FORWARD -> {
                drawLine(tint, Offset(w * 0.28f, h * 0.18f), Offset(w * 0.65f, h * 0.5f), stroke, StrokeCap.Round)
                drawLine(tint, Offset(w * 0.65f, h * 0.5f), Offset(w * 0.28f, h * 0.82f), stroke, StrokeCap.Round)
            }
        }
    }
}
