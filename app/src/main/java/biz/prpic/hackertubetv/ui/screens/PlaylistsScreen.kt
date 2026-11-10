package biz.prpic.hackertubetv.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Text
import biz.prpic.hackertubetv.AppViewModel
import biz.prpic.hackertubetv.data.PlaylistSummary
import biz.prpic.hackertubetv.data.UserLibrary
import biz.prpic.hackertubetv.format.formatHours
import biz.prpic.hackertubetv.nav.Screen
import biz.prpic.hackertubetv.ui.AppColors
import biz.prpic.hackertubetv.ui.LoadedScreen
import biz.prpic.hackertubetv.ui.rememberListFocus

private const val TILES_PER_ROW = 4
private val TileGap = 16.dp

// A tile's mosaic cell is a quarter of a card, so its images are decoded at about half a
// card's size.
private const val TILE_CELL_WIDTH_PX = 200
private const val TILE_CELL_HEIGHT_PX = 113

/** The playlists split the way the site shows them. A missing tier counts as deep. */
fun splitPlaylists(playlists: List<PlaylistSummary>): Pair<List<PlaylistSummary>, List<PlaylistSummary>> =
    playlists.partition { it.tier == "start" }

/** The grey line under a playlist tile: "12 videos · 2 hr 5 min". */
fun playlistTileLine(playlist: PlaylistSummary): String =
    "${playlist.count} videos · ${formatHours(playlist.minutes)}"

/** One entry of the vertical list: a section heading or one row of tiles. */
internal sealed interface Entry {
    val key: String

    class Section(override val key: String, val title: String, val description: String) : Entry

    /** Tiles numbered from [firstIndex] across the whole screen. */
    class Tiles(override val key: String, val firstIndex: Int, val tiles: List<PlaylistSummary>) : Entry
}

internal fun buildEntries(playlists: List<PlaylistSummary>): List<Entry> {
    val (start, deep) = splitPlaylists(playlists)
    val entries = ArrayList<Entry>()
    var next = 0
    fun section(key: String, title: String, description: String, items: List<PlaylistSummary>) {
        if (items.isEmpty()) return
        entries.add(Entry.Section(key, title, description))
        items.chunked(TILES_PER_ROW).forEachIndexed { row, tiles ->
            entries.add(Entry.Tiles("$key:$row", next, tiles))
            next += tiles.size
        }
    }
    section("start", "Start here", "Broad, essential lists for your first visit.", start)
    section("deep", "Deep dives", "One obsession each, for when you want to go down a rabbit hole.", deep)
    return entries
}

/** The list entry holding the tile numbered [focusIndex], or -1 when there is none. */
internal fun entryIndexOf(entries: List<Entry>, focusIndex: Int): Int = entries.indexOfFirst {
    it is Entry.Tiles && focusIndex >= it.firstIndex && focusIndex < it.firstIndex + it.tiles.size
}

/** Every playlist as a tile, in a "Start here" and a "Deep dives" section. */
@Composable
fun PlaylistsScreen(viewModel: AppViewModel, library: UserLibrary, onOpen: (Screen) -> Unit) {
    val screen = Screen.Playlists
    Column(Modifier.fillMaxSize()) {
        ScreenHeading(
            "Playlists",
            subtitle = "Curated by Claude from what Hacker News recommends, each in a deliberate order.",
        )
        LoadedScreen(load = { viewModel.playlists() }) { playlists, movesAtLoad ->
            if (playlists.isEmpty()) {
                Text(
                    text = "Coming soon.",
                    modifier = Modifier.padding(start = 8.dp, top = 12.dp),
                    color = AppColors.Secondary,
                    fontSize = 14.sp,
                )
            } else {
                PlaylistList(
                    entries = remember(playlists) { buildEntries(playlists) },
                    stopCount = playlists.size,
                    showFrames = library.showFrames,
                    initialFocus = remember { viewModel.focusIndex(screen) },
                    movesBefore = movesAtLoad,
                    onFocusChange = { viewModel.setFocusIndex(screen, it) },
                    onOpen = { onOpen(Screen.PlaylistDetail(it.slug, it.title)) },
                )
            }
        }
    }
}

@Composable
private fun PlaylistList(
    entries: List<Entry>,
    stopCount: Int,
    showFrames: Boolean,
    initialFocus: Int,
    movesBefore: Int,
    onFocusChange: (Int) -> Unit,
    onOpen: (PlaylistSummary) -> Unit,
) {
    val listState = rememberLazyListState()
    val focus = rememberListFocus(
        listState = listState,
        stopCount = stopCount,
        itemOf = { entryIndexOf(entries, it) },
        initialFocus = initialFocus,
        movesBefore = movesBefore,
        onFocusChange = onFocusChange,
    )
    with(focus) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .restoreFocusFromMenu(),
            contentPadding = PaddingValues(start = 8.dp, top = 8.dp, end = 32.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(TileGap),
        ) {
            entries.forEach { entry ->
                item(key = entry.key) {
                    when (entry) {
                        is Entry.Section -> Column {
                            Text(text = entry.title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = entry.description, color = AppColors.Secondary, fontSize = 14.sp)
                        }
                        is Entry.Tiles -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(TileGap)) {
                            entry.tiles.forEachIndexed { offset, playlist ->
                                PlaylistTile(
                                    playlist = playlist,
                                    showFrames = showFrames,
                                    onClick = { onOpen(playlist) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .focusStop(entry.firstIndex + offset),
                                )
                            }
                            // A short last row keeps its tiles the same width as the others.
                            repeat(TILES_PER_ROW - entry.tiles.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistTile(
    playlist: PlaylistSummary,
    showFrames: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = AppColors.Text,
            focusedContainerColor = Color.Transparent,
            focusedContentColor = AppColors.Text,
            pressedContainerColor = Color.Transparent,
            pressedContentColor = AppColors.Text,
        ),
        scale = CardDefaults.scale(focusedScale = 1.04f),
        border = CardDefaults.border(focusedBorder = Border.None),
        interactionSource = interactionSource,
    ) {
        PlaylistMosaic(
            vids = playlist.cover,
            showFrames = showFrames,
            cellWidthPx = TILE_CELL_WIDTH_PX,
            cellHeightPx = TILE_CELL_HEIGHT_PX,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (focused) Modifier.border(3.dp, AppColors.FocusFrame, MosaicShape) else Modifier),
        )
        Text(
            text = playlist.title,
            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 18.sp,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = playlistTileLine(playlist),
            color = AppColors.Secondary,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            maxLines = 1,
        )
    }
}
