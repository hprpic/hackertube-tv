package biz.prpic.hackertubetv.nav

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** Every screen the app can show. Serializable so the open screens survive process death. */
@Serializable
sealed interface Screen {
    @Serializable data object Home : Screen

    @Serializable data object Longs : Screen

    @Serializable data object Liked : Screen

    @Serializable data object History : Screen

    @Serializable data object WatchLater : Screen

    @Serializable data object Categories : Screen

    @Serializable data object Playlists : Screen

    @Serializable data object Search : Screen

    @Serializable data object Settings : Screen

    @Serializable data class Area(val id: String, val name: String) : Screen

    @Serializable data class Category(val id: String, val name: String, val areaName: String) : Screen

    /** A video list with the given site filters, for example `gems=1`. */
    @Serializable data class Browse(val title: String, val filters: Map<String, String>) : Screen

    @Serializable data class PlaylistDetail(val slug: String, val title: String) : Screen
}

/** The left menu, in the site's order. */
enum class RailItem(val label: String, val screen: Screen) {
    SEARCH("Search", Screen.Search),
    HOME("Home", Screen.Home),
    LONGS("Longs", Screen.Longs),
    LIKED("Liked", Screen.Liked),
    HISTORY("History", Screen.History),
    WATCH_LATER("Watch later", Screen.WatchLater),
    CATEGORIES("Categories", Screen.Categories),
    PLAYLISTS("Playlists", Screen.Playlists),
    SETTINGS("Settings", Screen.Settings),
}

/** What the activity has to do after the Back key; the stack is already updated. */
enum class BackAction { POP, FOCUS_RAIL, GO_HOME, EXIT }

/**
 * The open screens, root first. The root is always a menu screen; deeper screens (an
 * area, a category, a playlist) are opened on top of it.
 *
 * Back rules: from content, Back closes the top screen, or on a root screen moves focus
 * to the menu. From the menu, Back goes to Home, and on Home it leaves the app.
 */
class BackStack(initial: List<Screen> = listOf(Screen.Home)) {
    private val stack = initial.toMutableList()

    init {
        require(stack.isNotEmpty() && RailItem.entries.any { it.screen == stack.first() }) {
            "a back stack must start with a menu screen: $initial"
        }
    }

    val screens: List<Screen> get() = stack.toList()

    val current: Screen get() = stack.last()

    /** The menu item to highlight: the one whose screen is the root of the stack. */
    val railItem: RailItem get() = RailItem.entries.first { it.screen == stack.first() }

    fun open(screen: Screen) {
        stack.add(screen)
    }

    fun selectRail(item: RailItem) {
        stack.clear()
        stack.add(item.screen)
    }

    fun onBack(focusInRail: Boolean): BackAction = when {
        !focusInRail && stack.size > 1 -> {
            stack.removeAt(stack.lastIndex)
            BackAction.POP
        }
        !focusInRail -> BackAction.FOCUS_RAIL
        stack == listOf(Screen.Home) -> BackAction.EXIT
        else -> {
            selectRail(RailItem.HOME)
            BackAction.GO_HOME
        }
    }

    fun serialize(): String = Json.encodeToString(screenListSerializer, stack)

    companion object {
        private val screenListSerializer = ListSerializer(Screen.serializer())

        fun deserialize(text: String): BackStack = BackStack(Json.decodeFromString(screenListSerializer, text))

        /**
         * Restores a saved stack, or starts on Home when there is none or it cannot be read
         * (for example a stack saved by an older version of the app).
         */
        fun deserializeOrDefault(text: String?): BackStack {
            if (text.isNullOrBlank()) return BackStack()
            return try {
                deserialize(text)
            } catch (e: Exception) {
                BackStack()
            }
        }
    }
}
