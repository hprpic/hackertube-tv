package biz.prpic.hackertubetv.ui

import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusEnterExitScope

/**
 * For a focus group's onExit: Up and Down never leave the group, so at its top or bottom
 * edge focus stays where it is. Left and Right still leave it, and so does focus asked for
 * in code.
 */
fun FocusEnterExitScope.keepUpAndDownInside() {
    if (requestedFocusDirection == FocusDirection.Up || requestedFocusDirection == FocusDirection.Down) {
        cancelFocusChange()
    }
}
