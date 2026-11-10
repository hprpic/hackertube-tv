package biz.prpic.hackertubetv.paging

import biz.prpic.hackertubetv.data.Video
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Start loading the next page once focus is this close to the last loaded card. */
const val PREFETCH_DISTANCE = 8

/**
 * One loaded page. [consumed] is how far the offset moves, which can be more than
 * [items] when the source skipped entries. A page with [consumed] 0 means the end.
 */
data class Page(val items: List<Video>, val consumed: Int)

data class PagerState(
    val items: List<Video> = emptyList(),
    val loading: Boolean = false,
    val endReached: Boolean = false,
    val error: Boolean = false,
)

/**
 * Offset paging for a video grid. Loads run in [scope], one at a time. Call the
 * functions from the scope's own thread (the main thread in the app); the guard against
 * a second load relies on that.
 */
class VideoPager(
    private val scope: CoroutineScope,
    private val load: suspend (offset: Int) -> Page,
) {
    private val mutableState = MutableStateFlow(PagerState())
    val state: StateFlow<PagerState> = mutableState.asStateFlow()

    private var nextOffset = 0
    private var loadJob: Job? = null
    private val shownVids = HashSet<String>()

    /** Loads the first page unless something is already loaded. */
    fun start() {
        if (nextOffset == 0 && state.value.items.isEmpty()) loadNext()
    }

    /** Prefetches the next page when the focused card is near the end of what is loaded. */
    fun onFocused(index: Int) {
        val current = state.value
        // After a failure only an explicit retry loads again, so a dead network is not
        // hit on every focus move.
        if (current.error) return
        if (index >= current.items.size - PREFETCH_DISTANCE) loadNext()
    }

    /** Repeats the failed load from the same offset. */
    fun retry() {
        if (state.value.error) loadNext()
    }

    /**
     * Loads pages until the card at [index] exists, the list ends or a load fails. Used to
     * bring back the focused card after the app was recreated.
     */
    suspend fun loadUntil(index: Int) {
        while (true) {
            val current = state.value
            if (current.items.size > index || current.endReached || current.error) return
            loadNext()
            val job = loadJob ?: return
            job.join()
            if (job.isCancelled) return
        }
    }

    private fun loadNext() {
        val current = state.value
        if (current.loading || current.endReached) return
        // Set before the coroutine starts, so a second call in the meantime sees it.
        mutableState.value = current.copy(loading = true, error = false)
        loadJob = scope.launch {
            try {
                loadUntilSomethingNew()
            } catch (e: CancellationException) {
                mutableState.update { it.copy(loading = false) }
                throw e
            } catch (e: Exception) {
                // Network failures and unreadable answers alike: keep what is shown and
                // let the screen offer a retry.
                mutableState.update { it.copy(loading = false, error = true) }
            }
        }
    }

    // A page can move the offset on without adding a card: saved ids the site no longer
    // has, or videos already shown. With no new card there is nothing new to focus, so
    // focus would never ask for the next page; the pager moves on by itself instead.
    // A source that keeps answering with nothing new counts as ended after a few pages,
    // so it cannot turn into a crawl.
    private suspend fun loadUntilSomethingNew() {
        repeat(MAX_PAGES_WITHOUT_NEW_VIDEOS) {
            val page = load(nextOffset)
            nextOffset += page.consumed
            val fresh = page.items.filter { shownVids.add(it.vid) }
            if (fresh.isNotEmpty() || page.consumed == 0) {
                mutableState.update {
                    it.copy(items = it.items + fresh, loading = false, endReached = page.consumed == 0)
                }
                return
            }
        }
        mutableState.update { it.copy(loading = false, endReached = true) }
    }
}

/** Pages in a row that may add no new video before the list is treated as ended. */
private const val MAX_PAGES_WITHOUT_NEW_VIDEOS = 5
