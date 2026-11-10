package biz.prpic.hackertubetv.ui

import android.view.KeyEvent
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

private val ArrowKeys = setOf(
    KeyEvent.KEYCODE_DPAD_UP,
    KeyEvent.KEYCODE_DPAD_DOWN,
    KeyEvent.KEYCODE_DPAD_LEFT,
    KeyEvent.KEYCODE_DPAD_RIGHT,
)

/**
 * Counts the times the viewer moves focus with the remote. A grid that is still loading
 * the card it wants to focus notes the count when it asks, and gives up once the count
 * has changed, so a slow load cannot pull focus back from where the viewer went.
 */
@Stable
class FocusMoves {
    var count by mutableIntStateOf(0)
        private set

    fun record() {
        count++
    }

    /** Records a press of an arrow key, the keys that move focus. */
    fun recordKey(event: KeyEvent) {
        if (event.action == KeyEvent.ACTION_DOWN && event.keyCode in ArrowKeys) record()
    }
}

val LocalFocusMoves = staticCompositionLocalOf { FocusMoves() }
