@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
package dev.wolly.dsbmaterial.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicatorDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toPath
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.Morph
import dev.wolly.dsbmaterial.ui.theme.springDefaultSpatial
import dev.wolly.dsbmaterial.ui.theme.springDefaultEffects
import dev.wolly.dsbmaterial.ui.feedback
import kotlin.math.roundToInt
import kotlin.math.max
import kotlin.math.min

@Composable
fun CollapsingTopBar(
    title: String,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(64.dp)
                .padding(horizontal = 20.dp)
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLargeEmphasized,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                content = actions
            )
        }
    }
}

@Composable
fun ExpressiveSwitch(
    checked: Boolean,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier
) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
        animationSpec = springDefaultEffects(),
        label = "switch_track_color"
    )
    val borderColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        animationSpec = springDefaultEffects(),
        label = "switch_border_color"
    )
    val thumbColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline,
        animationSpec = springDefaultEffects(),
        label = "switch_thumb_color"
    )
    val iconAlpha by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = springDefaultEffects(),
        label = "switch_icon_alpha"
    )
    val thumbScale by animateFloatAsState(
        targetValue = if (checked) 1f else 0.9f,
        animationSpec = springDefaultSpatial(),
        label = "switch_thumb_scale"
    )
    val trackWidth = 56.dp
    val trackHeight = 32.dp
    val thumbSize = 24.dp
    val padding = 4.dp
    val haptics = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .size(trackWidth, trackHeight)
            .clip(RoundedCornerShape(16.dp))
            .background(trackColor)
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .semantics {
                role = Role.Switch
                toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
            }
            .clickable {
                haptics.feedback(HapticFeedbackType.LongPress)
                onCheckedChange()
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val thumbX by animateDpAsState(
            targetValue = if (checked) trackWidth - thumbSize - padding else padding,
            animationSpec = springDefaultSpatial(),
            label = "thumb_x"
        )
        Box(
            modifier = Modifier
                .offset(x = thumbX)
                .size(thumbSize)
                .graphicsLayer {
                    scaleX = thumbScale
                    scaleY = thumbScale
                }
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(thumbColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (checked) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                modifier = Modifier.size(14.dp).alpha(iconAlpha),
                tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun FontSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    displayValue: (Float) -> String,
    onValueChangeFinished: () -> Unit = {}
) {
    val haptics = LocalHapticFeedback.current
    val hasSteps = steps > 0
    val tickCount = steps + 2
    val span = valueRange.endInclusive - valueRange.start
    val tickIndex = when {
        hasSteps && span != 0f ->
            ((((value - valueRange.start) / span) * (tickCount - 1)).roundToInt()).coerceIn(0, tickCount - 1)
        !hasSteps && span >= 1f -> (value - valueRange.start).roundToInt()
        else -> 0
    }
    var lastTickIndex by remember { mutableStateOf(tickIndex) }
    LaunchedEffect(tickIndex) {
        if (tickIndex != lastTickIndex) {
            haptics.feedback(HapticFeedbackType.TextHandleMove)
            lastTickIndex = tickIndex
        }
    }

    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = displayValue(value),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = {
                onValueChangeFinished()
                if (!hasSteps) haptics.feedback(HapticFeedbackType.LongPress)
            },
            valueRange = valueRange,
            steps = steps,
            thumb = { sliderState ->
                val span = valueRange.endInclusive - valueRange.start
                val progress =
                    if (span == 0f) 0f
                    else ((sliderState.value - valueRange.start) / span).coerceIn(0f, 1f)
                MorphingSliderThumb(
                    progress = progress,
                    modifier = Modifier.size(40.dp)
                )
            }
        )
    }
}

/**
 * The font-roundness slider thumb: the M3 determinate morphing shape (circle → soft burst) drawn
 * without any clip, sitting on a soft background disc so the slider track stays readable behind it.
 */
@Composable
private fun MorphingSliderThumb(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val polygons = LoadingIndicatorDefaults.DeterminateIndicatorPolygons
    val morph = remember(polygons) {
        Morph(polygons[1].normalized(), polygons[0].normalized())
    }
    val scaleFactor = remember(polygons) {
        var factor = 1f
        val bounds = FloatArray(4)
        val maxBounds = FloatArray(4)
        polygons.forEach { polygon ->
            polygon.calculateBounds(bounds)
            polygon.calculateMaxBounds(maxBounds)
            val sx = (bounds[2] - bounds[0]) / (maxBounds[2] - maxBounds[0])
            val sy = (bounds[3] - bounds[1]) / (maxBounds[3] - maxBounds[1])
            factor = min(factor, max(sx, sy))
        }
        factor
    }
    val path = remember { Path() }
    val scaleMatrix = remember { Matrix() }
    val coerced = progress.coerceIn(0f, 1f)
    val shapeColor = MaterialTheme.colorScheme.primary
    val strokeWidth = 3.dp
    Canvas(modifier = modifier) {
        rotate(degrees = -coerced * 180, pivot = center) {
            val shapePath = morph.toPath(progress = coerced, path = path, startAngle = 0)
            scaleMatrix.reset()
                scaleMatrix.apply {
                    scale(
                        x = size.width * scaleFactor * 0.6f,
                        y = size.height * scaleFactor * 0.6f
                    )
                }
            shapePath.transform(scaleMatrix)
            shapePath.translate(center - shapePath.getBounds().center)
            drawPath(shapePath, color = shapeColor.copy(alpha = 0.12f))
            drawPath(
                path = shapePath,
                color = shapeColor,
                style = Stroke(width = strokeWidth.toPx())
            )
        }
    }
}

