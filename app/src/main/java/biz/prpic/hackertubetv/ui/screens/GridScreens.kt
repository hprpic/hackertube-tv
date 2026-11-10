package biz.prpic.hackertubetv.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import biz.prpic.hackertubetv.AppViewModel
import biz.prpic.hackertubetv.data.UserLibrary
import biz.prpic.hackertubetv.data.Video
import biz.prpic.hackertubetv.nav.Screen
import biz.prpic.hackertubetv.paging.VideoPager
import biz.prpic.hackertubetv.ui.AppColors
import biz.prpic.hackertubetv.ui.DEFAULT_EMPTY_TEXT
import biz.prpic.hackertubetv.ui.VideoActionsDialog
import biz.prpic.hackertubetv.ui.VideoGrid

private val BrandLine = buildAnnotatedString {
    append("Hacker")
    withStyle(SpanStyle(color = AppColors.Accent)) { append("Tube") }
}

/** The Home feed under the "HackerTube" brand line, as on the site's home page. */
@Composable
fun HomeScreen(viewModel: AppViewModel, library: UserLibrary) {
    val pager = remember { viewModel.pagerFor(Screen.Home) }
    GridScreen(viewModel, library, Screen.Home, pager) { ScreenHeading(BrandLine) }
}

/** Every video over an hour. */
@Composable
fun LongsScreen(viewModel: AppViewModel, library: UserLibrary) {
    val pager = remember { viewModel.pagerFor(Screen.Longs) }
    GridScreen(viewModel, library, Screen.Longs, pager) {
        ScreenHeading("Longs", subtitle = "Every video over an hour. The opposite of Shorts.")
    }
}

/** Every video of one area. */
@Composable
fun AreaScreen(viewModel: AppViewModel, library: UserLibrary, screen: Screen.Area) {
    val pager = remember { viewModel.pagerFor(screen) }
    GridScreen(viewModel, library, screen, pager) { ScreenHeading(screen.name) }
}

/** Every video of one category, with its area named above it. */
@Composable
fun CategoryScreen(viewModel: AppViewModel, library: UserLibrary, screen: Screen.Category) {
    val pager = remember { viewModel.pagerFor(screen) }
    GridScreen(viewModel, library, screen, pager) { ScreenHeading(screen.name, kicker = screen.areaName) }
}

/** A list opened from a quick link on the Categories screen. */
@Composable
fun BrowseScreen(viewModel: AppViewModel, library: UserLibrary, screen: Screen.Browse) {
    val pager = remember { viewModel.pagerFor(screen) }
    GridScreen(viewModel, library, screen, pager) { ScreenHeading(screen.title) }
}

@Composable
fun LikedScreen(viewModel: AppViewModel, library: UserLibrary) {
    SavedListScreen(
        viewModel, library, Screen.Liked,
        emptyText = "Videos you like will show up here, and shape your Home feed.",
    ) { ScreenHeading("Liked videos") }
}

@Composable
fun WatchLaterScreen(viewModel: AppViewModel, library: UserLibrary) {
    SavedListScreen(
        viewModel, library, Screen.WatchLater,
        emptyText = "Save videos to watch later: hold OK on a video.",
    ) { ScreenHeading("Watch later") }
}

/**
 * Liked or Watch later. Taking a video off this list from its menu, Unlike on Liked or
 * Remove from Watch later on Watch later, makes the view model rebuild the list, and the
 * grid starts again from it without that card.
 */
@Composable
private fun SavedListScreen(
    viewModel: AppViewModel,
    library: UserLibrary,
    screen: Screen,
    emptyText: String,
    heading: @Composable () -> Unit,
) {
    var pager by remember { mutableStateOf(viewModel.pagerFor(screen)) }
    GridScreen(
        viewModel, library, screen, pager,
        emptyText = emptyText,
        onMenuAction = { pager = viewModel.pagerFor(screen) },
        heading = heading,
    )
}

/**
 * The opened videos, newest first, with a button that clears them. Clearing empties the
 * screen at once; [focusMenu] then takes focus away from the button, which disappears.
 * Left from the button also goes to the menu, as from the first column of cards.
 */
@Composable
fun HistoryScreen(viewModel: AppViewModel, library: UserLibrary, focusMenu: () -> Unit) {
    var pager by remember { mutableStateOf(viewModel.pagerFor(Screen.History)) }
    // Hides the button at once, before the cleared library has been written.
    var cleared by remember { mutableStateOf(false) }
    val canClear = !cleared && library.seen.isNotEmpty()
    GridScreen(
        viewModel, library, Screen.History, pager,
        emptyText = "Videos you open will show up here.",
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ScreenHeading("History")
            if (canClear) {
                Button(
                    onClick = {
                        viewModel.clearHistory()
                        pager = viewModel.pagerFor(Screen.History)
                        cleared = true
                        focusMenu()
                    },
                    modifier = Modifier
                        .padding(start = 24.dp)
                        // Without this, the search for a target on the left finds the first card below.
                        .onPreviewKeyEvent {
                            val left = it.type == KeyEventType.KeyDown && it.key == Key.DirectionLeft
                            if (left) focusMenu()
                            left
                        },
                ) {
                    Text("Clear all watch history")
                }
            }
        }
    }
}

/**
 * A heading above a video grid, and the long-press menu for its cards. A short press
 * plays the video; a long press opens the menu, whose changes show the next time the
 * screen opens, except where [onMenuAction] picks up a list the change rebuilt.
 */
@Composable
private fun GridScreen(
    viewModel: AppViewModel,
    library: UserLibrary,
    screen: Screen,
    pager: VideoPager,
    emptyText: String = DEFAULT_EMPTY_TEXT,
    onMenuAction: () -> Unit = {},
    heading: @Composable () -> Unit,
) {
    // Read again for a replaced list, which starts at the position the view model kept.
    val initialFocus = remember(pager) { viewModel.focusIndex(screen) }
    // The video whose menu is open, if any.
    var menuFor by remember { mutableStateOf<Video?>(null) }
    Column(Modifier.fillMaxSize()) {
        heading()
        // A replaced list, as after clearing History, starts a fresh grid.
        key(pager) {
            VideoGrid(
                pager = pager,
                library = library,
                initialFocus = initialFocus,
                onFocusChange = { viewModel.setFocusIndex(screen, it) },
                onPlay = { viewModel.play(it.vid) },
                onLongPress = { menuFor = it },
                emptyText = emptyText,
            )
        }
    }
    menuFor?.let { video ->
        VideoActionsDialog(
            video = video,
            library = library,
            onDismiss = { menuFor = null },
            onToggleLike = {
                viewModel.toggleLike(video.vid)
                onMenuAction()
            },
            onToggleLater = {
                viewModel.toggleLater(video.vid)
                onMenuAction()
            },
            onToggleDislike = { viewModel.toggleDislike(video.vid) },
        )
    }
}
