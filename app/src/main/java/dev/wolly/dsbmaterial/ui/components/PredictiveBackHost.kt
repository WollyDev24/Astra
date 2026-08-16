package dev.wolly.dsbmaterial.ui.components

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import dev.wolly.dsbmaterial.ui.theme.springDefaultSpatial
import dev.wolly.dsbmaterial.ui.feedback
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch

/**
 * Wraps a sub-screen so the system predictive back gesture animates it away
 * (slide toward the swipe edge + shrink down with a growing corner radius)
 * while the page behind it fades in from a dimmed scrim, before calling [onBack].
 * Falls back to an immediate [onBack] on devices/back-presses without a gesture.
 */
@Composable
fun PredictiveBackHost(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val progress = remember { Animatable(0f) }
    var swipeEdge by remember { mutableStateOf(BackEventCompat.EDGE_LEFT) }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    PredictiveBackHandler { progressFlow ->
        try {
            var lastThreshold = 0
            progressFlow.collect { event ->
                swipeEdge = event.swipeEdge
                progress.snapTo(event.progress)
                val threshold = when {
                    event.progress >= 0.85f -> 3
                    event.progress >= 0.55f -> 2
                    event.progress >= 0.25f -> 1
                    else -> 0
                }
                if (threshold != lastThreshold) {
                    lastThreshold = threshold
                    haptics.feedback(HapticFeedbackType.TextHandleMove)
                }
            }
            haptics.feedback(HapticFeedbackType.LongPress)
            scope.launch {
                progress.animateTo(1f, tween(220))
                onBack()
            }
        } catch (e: CancellationException) {
            scope.launch {
                progress.animateTo(0f, springDefaultSpatial())
            }
            throw e
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.5f * (1f - progress.value) }
                .background(Color.Black)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val p = progress.value
                    val direction = if (swipeEdge == BackEventCompat.EDGE_RIGHT) -1f else 1f
                    translationX = direction * p * size.width * 0.35f
                    scaleX = 1f - p * 0.12f
                    scaleY = 1f - p * 0.12f
                    shape = RoundedCornerShape(48.dp * p)
                    clip = true
                }
        ) {
            content()
        }
    }
}
