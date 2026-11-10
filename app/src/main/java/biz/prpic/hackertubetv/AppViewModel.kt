package biz.prpic.hackertubetv

import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import biz.prpic.hackertubetv.data.Area
import biz.prpic.hackertubetv.data.HackerTubeApi
import biz.prpic.hackertubetv.data.Playlist
import biz.prpic.hackertubetv.data.PlaylistSummary
import biz.prpic.hackertubetv.data.UserLibrary
import biz.prpic.hackertubetv.data.UserLibraryStore
import biz.prpic.hackertubetv.nav.BackAction
import biz.prpic.hackertubetv.nav.BackStack
import biz.prpic.hackertubetv.nav.RailItem
import biz.prpic.hackertubetv.nav.Screen
import biz.prpic.hackertubetv.paging.Page
import biz.prpic.hackertubetv.paging.SearchSession
import biz.prpic.hackertubetv.paging.VideoPager
import biz.prpic.hackertubetv.paging.feedLoader
import biz.prpic.hackertubetv.paging.listLoader
import biz.prpic.hackertubetv.paging.savedIds
import biz.prpic.hackertubetv.paging.savedListLoader
import biz.prpic.hackertubetv.youtube.playAllUri
import biz.prpic.hackertubetv.youtube.watchUri
import biz.prpic.hackertubetv.youtube.youTubeIntent
import java.io.IOException
import kotlin.random.Random
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/** A read-only copy of the back stack for the UI: the open screens, root first. */
data class NavState(val screens: List<Screen>, val railItem: RailItem) {
    val current: Screen get() = screens.last()

    /** True when the menu item's own screen is shown, with nothing opened on top of it. */
    fun isShowing(item: RailItem): Boolean = screens == listOf(item.screen)
}

/**
 * The app's state across screens: navigation, the saved library, one pager per open
 * screen and the focus position on each. Navigation, the feed seed and the focus
 * positions are kept in [SavedStateHandle], so they survive the TV killing the app while
 * YouTube plays.
 */
class AppViewModel(
    private val savedState: SavedStateHandle,
    private val store: UserLibraryStore,
    private val api: HackerTubeApi,
) : ViewModel() {
    /** Chosen once, so a Home feed rebuilt after process death comes back in the same order. */
    private val seed: String =
        savedState.get<String>(KEY_SEED) ?: Random.nextInt(Int.MAX_VALUE).toString().also { savedState[KEY_SEED] = it }

    private val stack = BackStack.deserializeOrDefault(savedState.get<String>(KEY_BACK_STACK))

    var backStack by mutableStateOf(stack.toNavState())
        private set

    private val mutableLibrary = MutableStateFlow(UserLibrary())
    val library: StateFlow<UserLibrary> = mutableLibrary.asStateFlow()

    // Completes when the library has been read from disk for the first time.
    private val libraryRead = CompletableDeferred<Unit>()

    private val pagers = HashMap<Screen, VideoPager>()

    /** The search last submitted on the Search screen, or null before one was. */
    var search by mutableStateOf<SearchSession?>(
        savedState.get<String>(KEY_SEARCH_QUERY)?.let { SearchSession(viewModelScope, api, it) },
    )
        private set

    private val playChannel = Channel<Intent>(Channel.BUFFERED)

    /** Intents for the activity to start, one per video to play. */
    val playRequests: Flow<Intent> = playChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            store.library.retryingReads(LIBRARY_RETRY_MILLIS).collect { value ->
                mutableLibrary.value = value
                libraryRead.complete(Unit)
            }
        }
    }

    fun selectRail(item: RailItem) {
        changeStack { selectRail(item) }
    }

    /** Opens a screen on top of the current one. */
    fun open(screen: Screen) {
        changeStack { open(screen) }
    }

    /** The areas with their categories, for the Categories screen. */
    suspend fun areas(): List<Area> = api.browse()

    /**
     * Searches for [query]. A blank query does nothing. The query is kept in
     * [SavedStateHandle], so a search is run again after process death.
     */
    fun submitSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        savedState[KEY_SEARCH_QUERY] = trimmed
        savedState.remove<Int>(focusKey(Screen.Search))
        search = SearchSession(viewModelScope, api, trimmed)
    }

    fun onBack(focusInRail: Boolean): BackAction = changeStack { onBack(focusInRail) }

    /** The pager of an open screen; the same one is returned while the screen stays open. */
    fun pagerFor(screen: Screen): VideoPager = pagers.getOrPut(screen) { VideoPager(viewModelScope, loaderFor(screen)) }

    fun focusIndex(screen: Screen): Int = savedState.get<Int>(focusKey(screen)) ?: 0

    fun setFocusIndex(screen: Screen, index: Int) {
        savedState[focusKey(screen)] = index
    }

    /** Adds the video to History and asks the activity to open it in YouTube. */
    fun play(vid: String) {
        // History is a convenience; the video plays even when it cannot be saved.
        updateLibrary { it.markSeen(vid) }
        playChannel.trySend(youTubeIntent(watchUri(vid)))
    }

    /** Starts the videos as one YouTube list; only the first is added to History. */
    fun playAll(vids: List<String>) {
        val first = vids.firstOrNull() ?: return
        updateLibrary { it.markSeen(first) }
        playChannel.trySend(youTubeIntent(playAllUri(vids)))
    }

    /** The playlists for the Playlists screen. */
    suspend fun playlists(): List<PlaylistSummary> = api.playlists()

    /** One playlist with its videos in order. */
    suspend fun playlist(slug: String): Playlist = api.playlist(slug)

    /**
     * Switches video frames on or off in place of the uploaders' thumbnails, on every screen.
     * The setting is flipped inside the store's update, so two quick presses flip it twice.
     */
    fun toggleShowFrames() = updateLibrary { it.withShowFrames(!it.showFrames) }

    /** Like or Unlike. Unlike on the Liked screen takes the card off that screen at once. */
    fun toggleLike(vid: String) = toggleSaved(Screen.Liked, vid) { it.toggleLike(vid) }

    /** Watch later or Remove from Watch later; removing on the Watch later screen takes the card off at once. */
    fun toggleLater(vid: String) = toggleSaved(Screen.WatchLater, vid) { it.toggleLater(vid) }

    /** Not interested: the next Home feed leaves the video, and ones like it, out. */
    fun toggleDislike(vid: String) = updateLibrary { it.toggleDislike(vid) }

    /**
     * Empties History. The open History screen gets an empty list at once, instead of
     * keeping the list it opened with as the other saved screens do.
     */
    fun clearHistory() {
        if (Screen.History in stack.screens) {
            pagers[Screen.History] = VideoPager(viewModelScope, savedListLoader(api, emptyList()))
            savedState.remove<Int>(focusKey(Screen.History))
        }
        updateLibrary { it.clearHistory() }
    }

    // A change that takes the video off [list] while that list is the screen shown rebuilds
    // the list from the changed ids at once, as clearing History does, so the card goes
    // away; focus keeps its position, now on the next card. A list that is not on screen
    // shows the change the next time it opens.
    private fun toggleSaved(list: Screen, vid: String, transform: (UserLibrary) -> UserLibrary) {
        val ids = savedIds(transform(library.value), list)
        if (stack.current == list && vid !in ids) {
            pagers[list] = VideoPager(viewModelScope, savedListLoader(api, ids))
            setFocusIndex(list, focusIndex(list).coerceIn(0, maxOf(ids.lastIndex, 0)))
        }
        updateLibrary(transform)
    }

    private fun updateLibrary(transform: (UserLibrary) -> UserLibrary) {
        viewModelScope.launch {
            try {
                store.update(transform)
            } catch (_: IOException) {
                // A change that cannot be written is lost; the app goes on with what is stored.
            }
        }
    }

    private fun loaderFor(screen: Screen): suspend (Int) -> Page = when (screen) {
        Screen.Home -> homeLoader()
        Screen.Longs -> listLoader(api, mapOf("dur" to "xlong"))
        Screen.Liked, Screen.History, Screen.WatchLater -> savedLoader(screen)
        is Screen.Area -> listLoader(api, mapOf("area" to screen.id))
        is Screen.Category -> listLoader(api, mapOf("cat" to screen.id))
        is Screen.Browse -> listLoader(api, screen.filters)
        else -> throw IllegalArgumentException("$screen has no video list yet")
    }

    // The first page waits for the library from disk, so even the first request carries
    // the likes, dislikes and history, and a feed rebuilt after process death matches.
    private fun homeLoader(): suspend (Int) -> Page {
        val feed = feedLoader(api, { library.value }, seed)
        return { offset ->
            libraryRead.await()
            feed(offset)
        }
    }

    // The saved ids are taken once, for the first page, so the list stays as it was when
    // the screen opened while likes and Watch later change around it. That first page
    // waits for the library from disk, or a screen restored after process death would
    // come back empty.
    private fun savedLoader(screen: Screen): suspend (Int) -> Page {
        var load: (suspend (Int) -> Page)? = null
        return { offset ->
            libraryRead.await()
            val saved = load ?: savedListLoader(api, savedIds(library.value, screen)).also { load = it }
            saved(offset)
        }
    }

    private fun <T> changeStack(change: BackStack.() -> T): T {
        val result = stack.change()
        backStack = stack.toNavState()
        savedState[KEY_BACK_STACK] = stack.serialize()
        forgetClosedScreens()
        return result
    }

    // Pagers and focus positions live only as long as their screen is on the stack.
    private fun forgetClosedScreens() {
        val open = stack.screens.toSet()
        pagers.keys.retainAll(open)
        if (Screen.Search !in open) {
            search = null
            savedState.remove<String>(KEY_SEARCH_QUERY)
        }
        val openFocusKeys = open.map(::focusKey).toSet()
        savedState.keys()
            .filter { it.startsWith(FOCUS_KEY_PREFIX) && it !in openFocusKeys }
            .forEach { savedState.remove<Int>(it) }
    }

    companion object {
        private const val KEY_SEED = "seed"
        private const val KEY_SEARCH_QUERY = "searchQuery"
        private const val KEY_BACK_STACK = "backStack"
        private const val FOCUS_KEY_PREFIX = "focus:"

        // How long to wait before reading the library again after the disk failed.
        private const val LIBRARY_RETRY_MILLIS = 2_000L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                AppViewModel(createSavedStateHandle(), AppGraph.libraryStore(app), AppGraph.api)
            }
        }

        private fun focusKey(screen: Screen): String =
            FOCUS_KEY_PREFIX + Json.encodeToString(Screen.serializer(), screen)
    }
}

private fun BackStack.toNavState() = NavState(screens, railItem)

/**
 * The library as read from disk, for as long as it is collected. A read that fails with an
 * IOException does not end it; reading starts again after [retryMillis]. Until a library
 * has been read an empty one stands in, so the app can start; after that the last one read
 * stays. Any other failure ends the flow.
 */
fun Flow<UserLibrary>.retryingReads(retryMillis: Long): Flow<UserLibrary> = flow {
    var emitted = false
    this@retryingReads.retryWhen { cause, _ ->
        if (cause !is IOException) return@retryWhen false
        if (!emitted) emit(UserLibrary())
        delay(retryMillis)
        true
    }.collect {
        emitted = true
        emit(it)
    }
}
