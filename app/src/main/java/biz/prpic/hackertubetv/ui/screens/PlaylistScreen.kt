package biz.prpic.hackertubetv.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import biz.prpic.hackertubetv.AppViewModel
import biz.prpic.hackertubetv.data.Playlist
import biz.prpic.hackertubetv.data.UserLibrary
import biz.prpic.hackertubetv.data.Video
import biz.prpic.hackertubetv.format.formatHours
import biz.prpic.hackertubetv.format.videoSubline
import biz.prpic.hackertubetv.nav.Screen
import biz.prpic.hackertubetv.ui.AppColors
import biz.prpic.hackertubetv.ui.LoadedScreen
import biz.prpic.hackertubetv.ui.VideoActionsDialog
import biz.prpic.hackertubetv.ui.VideoImage
import biz.prpic.hackertubetv.ui.rememberListFocus

private val RowShape = RoundedCornerShape(8.dp)

// The header mosaic is 280 dp wide, 560 px on the TV, so each of its cells is about
// 280 x 158 px.
private const val HEADER_CELL_WIDTH_PX = 280
private const val HEADER_CELL_HEIGHT_PX = 158

/** The grey line under a playlist's title: "Curated by Claude · 12 videos · 2 hr 5 min". */
fun playlistMetaLine(playlist: Playlist): String =
    "Curated by Claude · ${playlist.videos.size} videos · ${formatHours(playlist.minutes)}"

/**
 * One playlist: a mosaic, its title and intro, a Play all button and the videos in order.
 * Focus numbers: 0 is Play all, then 1 for the first video. A playlist without videos has
 * no Play all and so no focus stops.
 */
@Composable
fun PlaylistScreen(viewModel: AppViewModel, library: UserLibrary, screen: Screen.PlaylistDetail) {
    Column(Modifier.fillMaxSize()) {
        LoadedScreen(
            load = { viewModel.playlist(screen.slug) },
            notLoaded = { ScreenHeading(screen.title) },
        ) { playlist, movesAtLoad ->
            PlaylistPage(viewModel, library, screen, playlist, movesAtLoad)
        }
    }
}

@Composable
private fun PlaylistPage(viewModel: AppViewModel, library: UserLibrary, screen: Screen, playlist: Playlist, movesBefore: Int) {
    val listState = rememberLazyListState()
    val nowMillis = remember { System.currentTimeMillis() }
    var menuFor by remember { mutableStateOf<Video?>(null) }
    // Stop 0, Play all, sits in the header, list item 0; stop n is video n, list item n.
    val focus = rememberListFocus(
        listState = listState,
        stopCount = if (playlist.videos.isEmpty()) 0 else playlist.videos.size + 1,
        itemOf = { it },
        initialFocus = remember { viewModel.focusIndex(screen) },
        movesBefore = movesBefore,
        onFocusChange = { viewModel.setFocusIndex(screen, it) },
    )

    with(focus) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .restoreFocusFromMenu(),
            contentPadding = PaddingValues(start = 8.dp, top = 20.dp, end = 32.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "header") {
                PlaylistHeader(
                    playlist = playlist,
                    showFrames = library.showFrames,
                    playAllModifier = Modifier.focusStop(0),
                    onPlayAll = { viewModel.playAll(playlist.videos.map { it.vid }) },
                )
            }
            playlist.videos.forEachIndexed { position, video ->
                item(key = "video:$position:${video.vid}") {
                    VideoRow(
                        number = position + 1,
                        video = video,
                        showFrames = library.showFrames,
                        nowMillis = nowMillis,
                        onClick = { viewModel.play(video.vid) },
                        onLongClick = { menuFor = video },
                        modifier = Modifier.focusStop(position + 1),
                    )
                }
            }
        }
    }
    menuFor?.let { video ->
        VideoActionsDialog(
            video = video,
            library = library,
            onDismiss = { menuFor = null },
            onToggleLike = { viewModel.toggleLike(video.vid) },
            onToggleLater = { viewModel.toggleLater(video.vid) },
            onToggleDislike = { viewModel.toggleDislike(video.vid) },
        )
    }
}

/** The mosaic beside the title, the meta line, the intro and, with videos, the Play all button. */
@Composable
private fun PlaylistHeader(playlist: Playlist, showFrames: Boolean, playAllModifier: Modifier, onPlayAll: () -> Unit) {
    Row(Modifier.padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        PlaylistMosaic(
            vids = playlist.videos.map { it.vid },
            showFrames = showFrames,
            cellWidthPx = HEADER_CELL_WIDTH_PX,
            cellHeightPx = HEADER_CELL_HEIGHT_PX,
            modifier = Modifier.width(280.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(text = playlist.title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = playlistMetaLine(playlist),
                modifier = Modifier.padding(top = 4.dp),
                color = AppColors.Secondary,
                fontSize = 14.sp,
            )
            if (playlist.intro.isNotEmpty()) {
                Text(
                    text = playlist.intro,
                    modifier = Modifier.padding(top = 8.dp),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (playlist.videos.isNotEmpty()) {
                Button(onClick = onPlayAll, modifier = Modifier.padding(top = 12.dp).then(playAllModifier)) {
                    Text("Play all")
                }
            }
        }
    }
}

/** A numbered video: image, title, channel, views and age, and the note on why it is here. */
@Composable
private fun VideoRow(
    number: Int,
    video: Video,
    showFrames: Boolean,
    nowMillis: Long,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier,
) {
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier.fillMaxWidth(),
        shape = ClickableSurfaceDefaults.shape(RowShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = AppColors.Text,
            focusedContainerColor = AppColors.Raised,
            focusedContentColor = AppColors.Text,
            pressedContainerColor = AppColors.Raised,
            pressedContentColor = AppColors.Text,
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(3.dp, AppColors.FocusFrame), shape = RowShape),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = number.toString(),
                modifier = Modifier.width(32.dp),
                color = AppColors.Secondary,
                fontSize = 14.sp,
            )
            VideoImage(
                vid = video.vid,
                showFrames = showFrames,
                modifier = Modifier.width(160.dp).aspectRatio(16f / 9f),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOf(video.channel, videoSubline(video.views, video.published, nowMillis))
                        .filter { it.isNotEmpty() }
                        .joinToString(" • "),
                    color = AppColors.Secondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!video.note.isNullOrEmpty()) {
                    Text(
                        text = video.note,
                        modifier = Modifier.padding(top = 4.dp),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
