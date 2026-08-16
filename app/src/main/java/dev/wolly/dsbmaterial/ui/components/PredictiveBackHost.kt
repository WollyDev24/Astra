package dev.wolly.dsbmaterial.ui.components

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import dev.wolly.dsbmaterial.ui.theme.springDefaultSpatial
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch

/**
 * Wraps a sub-screen so the system predictive back gesture animates it away
 * (slide toward the swipe edge + fade + slight scale) before calling [onBack].
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

    PredictiveBackHandler { progressFlow ->
        try {
            progressFlow.collect { event ->
                swipeEdge = event.swipeEdge
                progress.snapTo(event.progress)
            }
            onBack()
        } catch (e: CancellationException) {
            scope.launch {
                progress.animateTo(0f, springDefaultSpatial())
            }
            throw e
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                val p = progress.value
                val direction = if (swipeEdge == BackEventCompat.EDGE_RIGHT) -1f else 1f
                translationX = direction * p * size.width * 0.35f
                scaleX = 1f - p * 0.08f
                scaleY = 1f - p * 0.08f
                alpha = 1f - p * 0.35f
            }
    ) {
        content()
    }
}
