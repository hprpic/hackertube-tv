package biz.prpic.hackertubetv.ui

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import kotlinx.coroutines.flow.first

/**
 * The stop that gets focus when a list appears: the stored one while the list still has
 * it, otherwise the first. Null for a list without stops.
 */
fun restoreTarget(initialFocus: Int, stopCount: Int): Int? = when {
    stopCount <= 0 -> null
    initialFocus in 0 until stopCount -> initialFocus
    else -> 0
}

/**
 * Focus in a lazy list whose focusable items, its stops, are numbered across the whole
 * screen. Each stop takes [focusStop], the list itself [restoreFocusFromMenu]. Made by
 * [rememberListFocus].
 */
@Stable
class ListFocus internal constructor(
    private val listState: LazyListState,
    initialFocus: Int,
    private val itemOf: (stop: Int) -> Int,
    private val onFocusChange: (stop: Int) -> Unit,
) {
    // One requester per stop, created when the stop is first shown.
    private val requesters = HashMap<Int, FocusRequester>()
    private var lastFocused = initialFocus

    /** Makes this the focus stop numbered [stop]; focusing it stores the number. */
    fun Modifier.focusStop(stop: Int): Modifier = this
        .focusRequester(requesters.getOrPut(stop) { FocusRequester() })
        .onFocusChanged {
            if (it.isFocused) {
                lastFocused = stop
                onFocusChange(stop)
            }
        }

    /**
     * For the list: coming back from the menu with Right lands on the stop that had focus,
     * while it is on screen. Only that move is redirected; focus asked for on a specific
     * stop goes where it was asked.
     */
    fun Modifier.restoreFocusFromMenu(): Modifier = this
        .focusProperties {
            onEnter = {
                if (requestedFocusDirection == FocusDirection.Right && isShown(lastFocused)) {
                    requesters[lastFocused]?.requestFocus()
                }
            }
        }
        .focusGroup()

    internal fun isShown(stop: Int): Boolean {
        val item = itemOf(stop)
        return item >= 0 && listState.layoutInfo.visibleItemsInfo.any { it.index == item }
    }

    /** Scrolls to [stop] and focuses it, unless the viewer moved focus since [movesBefore]. */
    internal suspend fun restore(stop: Int, movesBefore: Int, focusMoves: FocusMoves) {
        val item = itemOf(stop)
        if (item < 0) return
        if (listState.layoutInfo.visibleItemsInfo.none { it.index == item }) listState.scrollToItem(item)
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.any { it.index == item } }.first { it }
        if (focusMoves.count == movesBefore) requesters[stop]?.requestFocus()
    }
}

/**
 * The focus handling of a list with [stopCount] stops. [itemOf] gives the list item a stop
 * sits in, or -1. When the list appears, focus goes to [initialFocus], or to the first stop
 * when the list no longer has that one, so a screen comes back where it was after the app
 * was closed in the background. That is dropped once the focus move count differs from
 * [movesBefore], the count from when the screen started loading.
 */
@Composable
fun rememberListFocus(
    listState: LazyListState,
    stopCount: Int,
    itemOf: (stop: Int) -> Int,
    initialFocus: Int,
    movesBefore: Int,
    onFocusChange: (stop: Int) -> Unit,
): ListFocus {
    val currentItemOf by rememberUpdatedState(itemOf)
    val currentOnFocusChange by rememberUpdatedState(onFocusChange)
    val focus = remember(listState) {
        ListFocus(listState, initialFocus, { currentItemOf(it) }, { currentOnFocusChange(it) })
    }
    val focusMoves = LocalFocusMoves.current
    LaunchedEffect(focus) {
        val target = restoreTarget(initialFocus, stopCount) ?: return@LaunchedEffect
        focus.restore(target, movesBefore, focusMoves)
    }
    return focus
}
