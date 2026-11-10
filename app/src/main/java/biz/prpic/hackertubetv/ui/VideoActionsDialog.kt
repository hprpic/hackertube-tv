package biz.prpic.hackertubetv.ui

import android.view.KeyEvent as NativeKeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import biz.prpic.hackertubetv.data.UserLibrary
import biz.prpic.hackertubetv.data.Video

private val PanelShape = RoundedCornerShape(12.dp)

// The keys the TV buttons treat as OK.
private val OkKeys = setOf(NativeKeyEvent.KEYCODE_DPAD_CENTER, NativeKeyEvent.KEYCODE_ENTER, NativeKeyEvent.KEYCODE_NUMPAD_ENTER)

/**
 * The menu a long press of OK opens over the screen: Like, Watch later and Not
 * interested, each labelled by the video's current state. A button applies its change and
 * closes the menu; Back closes it with no change. The menu is its own window, so the card
 * behind it keeps focus and has it again once the menu closes.
 */
@Composable
fun VideoActionsDialog(
    video: Video,
    library: UserLibrary,
    onDismiss: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleLater: () -> Unit,
    onToggleDislike: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val firstButton = remember { FocusRequester() }
        val gate = remember { HeldOkGate() }
        Column(
            modifier = Modifier
                .onPreviewKeyEvent(gate::swallows)
                .width(360.dp)
                .background(AppColors.Background, PanelShape)
                .border(1.dp, AppColors.Raised, PanelShape)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = video.title,
                modifier = Modifier.padding(bottom = 4.dp),
                color = AppColors.Secondary,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            ActionButton(
                label = if (library.isLiked(video.vid)) "Unlike" else "Like",
                modifier = Modifier.focusRequester(firstButton),
            ) {
                onToggleLike()
                onDismiss()
            }
            ActionButton(if (library.isLater(video.vid)) "Remove from Watch later" else "Watch later") {
                onToggleLater()
                onDismiss()
            }
            ActionButton(if (library.isDisliked(video.vid)) "Remove from Not interested" else "Not interested") {
                onToggleDislike()
                onDismiss()
            }
        }
        LaunchedEffect(Unit) { firstButton.requestFocus() }
    }
}

@Composable
private fun ActionButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Text(label)
    }
}

/**
 * The OK key that opened the menu is still held when the menu appears, and its repeats
 * and its release arrive here. The release alone would press the focused button, so OK
 * events are swallowed until a new press of OK starts.
 */
private class HeldOkGate {
    private var open = false

    fun swallows(event: KeyEvent): Boolean {
        val native = event.nativeKeyEvent
        if (native.keyCode !in OkKeys) return false
        if (event.type == KeyEventType.KeyDown && native.repeatCount == 0) open = true
        return !open
    }
}
