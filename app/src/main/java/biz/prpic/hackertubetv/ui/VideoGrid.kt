package biz.prpic.hackertubetv.ui

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import biz.prpic.hackertubetv.data.UserLibrary
import biz.prpic.hackertubetv.data.Video
import biz.prpic.hackertubetv.paging.VideoPager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val COLUMNS = 4
private const val RETRY_KEY = "status:retry"

/** What an empty grid says when its screen has no text of its own. */
const val DEFAULT_EMPTY_TEXT = "Nothing here yet."

/**
 * A wish to put focus on the grid item at [index]. Each new object starts a new attempt.
 * [movesBefore] is the viewer's focus move count when it was made.
 */
private class FocusRequest(val index: Int, val movesBefore: Int)

/** Where focus was in the grid, kept outside Compose state because nothing redraws from it. */
private class FocusMemory(var lastFocused: Int) {
    var gridHasFocus = false
    var focusedAtPause = false
}

/**
 * A grid of video cards, four per row, that loads more as focus moves down.
 *
 * When the grid appears it loads up to [initialFocus] and puts focus on that card, so a
 * screen comes back where it was after the app was closed in the background. The same
 * happens when the app returns to the foreground with focus in the grid. Such a restore
 * is dropped once the viewer moves focus before it is done.
 */
@Composable
fun VideoGrid(
    pager: VideoPager,
    library: UserLibrary,
    initialFocus: Int,
    onFocusChange: (Int) -> Unit,
    onPlay: (Video) -> Unit,
    onLongPress: (Video) -> Unit,
    emptyText: String = DEFAULT_EMPTY_TEXT,
) {
    val state by pager.state.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()
    // One requester per card position, created when the card is first shown.
    val cardRequesters = remember { HashMap<Int, FocusRequester>() }
    val retryRequester = remember { FocusRequester() }
    val memory = remember { FocusMemory(initialFocus) }
    val nowMillis = remember { System.currentTimeMillis() }
    val focusMoves = LocalFocusMoves.current
    var focusRequest by remember { mutableStateOf<FocusRequest?>(FocusRequest(initialFocus, focusMoves.count)) }
    // Keeps the Retry row, and the focus on it, while the reload it started runs.
    var keepRetry by remember { mutableStateOf(false) }

    LaunchedEffect(pager) { pager.start() }

    LaunchedEffect(focusRequest) {
        val request = focusRequest ?: return@LaunchedEffect
        val restore = launch {
            pager.loadUntil(request.index)
            val loaded = pager.state.value
            val last = loaded.items.lastIndex
            when {
                request.index <= last ->
                    focusItem(gridState, request.index, loaded.items[request.index].vid) { cardRequesters[request.index] }
                // The Retry button sits right after the last card.
                loaded.error -> focusItem(gridState, loaded.items.size, RETRY_KEY) { retryRequester }
                // The list ended before the wanted card: the last card is the closest one.
                last >= 0 -> focusItem(gridState, last, loaded.items[last].vid) { cardRequesters[last] }
            }
        }
        // A move by the viewer ends the attempt, so a slow load cannot pull focus back
        // from where the viewer went in the meantime, for example into the menu.
        val overtaken = launch {
            snapshotFlow { focusMoves.count }.first { it != request.movesBefore }
            restore.cancel()
        }
        restore.join()
        overtaken.cancel()
        focusRequest = null
        keepRetry = false
    }

    LifecycleResumeEffect(pager) {
        if (memory.focusedAtPause) focusRequest = FocusRequest(memory.lastFocused, focusMoves.count)
        onPauseOrDispose { memory.focusedAtPause = memory.gridHasFocus }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(COLUMNS),
        state = gridState,
        modifier = Modifier
            .fillMaxSize()
            .onFocusChanged { memory.gridHasFocus = it.hasFocus }
            .focusProperties {
                // Coming back from the menu with Right lands on the card that was focused
                // before. Only that move is redirected: focus asked for on a specific card,
                // as after Retry or a restore, goes where it was asked.
                onEnter = {
                    val last = memory.lastFocused
                    val lastIsShown = last < state.items.size &&
                        gridState.layoutInfo.visibleItemsInfo.any { it.index == last }
                    if (requestedFocusDirection == FocusDirection.Right && lastIsShown) {
                        cardRequesters[last]?.requestFocus()
                    }
                }
            }
            .focusGroup(),
        contentPadding = PaddingValues(start = 8.dp, top = 8.dp, end = 32.dp, bottom = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        itemsIndexed(state.items, key = { _, video -> video.vid }) { index, video ->
            VideoCard(
                video = video,
                showFrames = library.showFrames,
                nowMillis = nowMillis,
                onClick = { onPlay(video) },
                onLongClick = { onLongPress(video) },
                modifier = Modifier
                    .focusRequester(cardRequesters.getOrPut(index) { FocusRequester() })
                    .onFocusChanged {
                        if (it.isFocused) {
                            memory.lastFocused = index
                            pager.onFocused(index)
                            onFocusChange(index)
                        }
                    },
            )
        }
        when {
            state.error || keepRetry -> item(key = RETRY_KEY, span = { GridItemSpan(maxLineSpan) }) {
                RetryMessage(
                    requester = retryRequester,
                    onRetry = {
                        if (pager.state.value.error) {
                            // Afterwards focus goes to the first new card, or to the card that
                            // was being restored; back to Retry if it fails again.
                            val target = maxOf(state.items.size, memory.lastFocused)
                            keepRetry = true
                            pager.retry()
                            focusRequest = FocusRequest(target, focusMoves.count)
                        }
                    },
                )
            }
            state.endReached && state.items.isEmpty() ->
                item(key = "status:empty", span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = emptyText,
                        modifier = Modifier.padding(vertical = 24.dp),
                        color = AppColors.Secondary,
                        fontSize = 14.sp,
                    )
                }
        }
    }
}

/**
 * Waits until the grid shows the item with [key] at [index], scrolling to it if needed,
 * then focuses it. The key makes sure it is that item and not a status row that sat at
 * the same index a frame earlier.
 */
private suspend fun focusItem(gridState: LazyGridState, index: Int, key: Any, requester: () -> FocusRequester?) {
    // The grid can be a frame behind the pager, so first wait for the index to exist.
    snapshotFlow { gridState.layoutInfo.totalItemsCount > index }.first { it }
    if (gridState.layoutInfo.visibleItemsInfo.none { it.key == key }) gridState.scrollToItem(index)
    snapshotFlow { gridState.layoutInfo.visibleItemsInfo.any { it.key == key } }.first { it }
    requester()?.requestFocus()
}

@Composable
internal fun RetryMessage(requester: FocusRequester, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "Could not reach hackertube.org.", color = AppColors.Secondary, fontSize = 14.sp)
        Button(
            onClick = onRetry,
            modifier = Modifier
                .padding(top = 12.dp)
                .focusRequester(requester),
        ) {
            Text("Retry")
        }
    }
}
