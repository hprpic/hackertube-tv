package biz.prpic.hackertubetv.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import java.io.IOException

/** Where a [LoadedScreen] is: still loading, loaded, or failed to load. */
private sealed interface LoadState<out T : Any> {
    data object Loading : LoadState<Nothing>

    data class Loaded<T : Any>(val value: T) : LoadState<T>

    data object Failed : LoadState<Nothing>
}

/**
 * Loads what a screen shows and then shows [content] with it. Until then it shows
 * [notLoaded]; after a failure the Retry message follows, and Retry loads again.
 *
 * [content] also gets the viewer's focus move count from when its load started. A screen
 * puts focus on its own item only while the count is unchanged, so arrows pressed during a
 * slow load, for example into the menu, win. Retry takes focus on the same terms.
 */
@Composable
fun <T : Any> LoadedScreen(
    load: suspend () -> T,
    notLoaded: @Composable () -> Unit = {},
    content: @Composable (value: T, movesAtLoad: Int) -> Unit,
) {
    val focusMoves = LocalFocusMoves.current
    val currentLoad by rememberUpdatedState(load)
    var state by remember { mutableStateOf<LoadState<T>>(LoadState.Loading) }
    // Bumped by Retry to load again.
    var attempt by remember { mutableIntStateOf(0) }
    var movesAtLoad by remember { mutableIntStateOf(focusMoves.count) }
    LaunchedEffect(attempt) {
        movesAtLoad = focusMoves.count
        state = try {
            LoadState.Loaded(currentLoad())
        } catch (e: IOException) {
            LoadState.Failed
        }
    }

    val current = state
    if (current is LoadState.Loaded) {
        content(current.value, movesAtLoad)
    } else {
        notLoaded()
        if (current == LoadState.Failed) {
            val requester = remember { FocusRequester() }
            RetryMessage(
                requester = requester,
                onRetry = {
                    state = LoadState.Loading
                    attempt++
                },
            )
            // The item that had focus is gone, so Retry takes it.
            LaunchedEffect(attempt) {
                if (focusMoves.count == movesAtLoad) requester.requestFocus()
            }
        }
    }
}
