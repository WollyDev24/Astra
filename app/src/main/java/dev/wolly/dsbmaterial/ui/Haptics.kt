package dev.wolly.dsbmaterial.ui

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

object Haptics {
    @Volatile
    var enabled: Boolean = true
        private set

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }
}

fun HapticFeedback.feedback(type: HapticFeedbackType) {
    if (Haptics.enabled) performHapticFeedback(type)
}
