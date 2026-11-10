package biz.prpic.hackertubetv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import biz.prpic.hackertubetv.AppViewModel
import biz.prpic.hackertubetv.data.Area
import biz.prpic.hackertubetv.format.formatCount
import biz.prpic.hackertubetv.nav.Screen
import biz.prpic.hackertubetv.ui.AppColors
import biz.prpic.hackertubetv.ui.LoadedScreen
import biz.prpic.hackertubetv.ui.rememberListFocus

private val ChipShape = RoundedCornerShape(10.dp)

/** A short link at the top of the screen. */
private class QuickLink(val label: String, val screen: Screen?)

private val QuickLinks = listOf(
    QuickLink("Recently recommended", Screen.Browse("Recently recommended", mapOf("sort" to "recent"))),
    // Longs is the menu's own Longs screen.
    QuickLink("Longs", null),
    QuickLink("Hidden gems", Screen.Browse("Hidden gems", mapOf("gems" to "1"))),
    QuickLink("All videos", Screen.Browse("All videos", emptyMap())),
)

/** One focusable thing on the screen. Its position in the list is the number focus is stored by. */
internal class Target(val label: String, val count: Int?, val screen: Screen?)

/** One list item: a row of targets, numbered from [firstIndex] across the whole screen. */
internal class ListRow(val key: String, val firstIndex: Int, val targets: List<Target>, val wrapping: Boolean)

internal fun buildRows(areas: List<Area>): List<ListRow> {
    val rows = ArrayList<ListRow>()
    var next = 0
    fun add(key: String, targets: List<Target>, wrapping: Boolean) {
        rows.add(ListRow(key, next, targets, wrapping))
        next += targets.size
    }
    add("quick", QuickLinks.map { Target(it.label, null, it.screen) }, wrapping = false)
    for (area in areas) {
        add("area:${area.id}", listOf(Target(area.name, area.count, Screen.Area(area.id, area.name))), wrapping = false)
        val chips = area.categories.map { Target(it.name, it.count, Screen.Category(it.id, it.name, area.name)) }
        if (chips.isNotEmpty()) add("chips:${area.id}", chips, wrapping = true)
    }
    return rows
}

/** The list item holding the target numbered [focusIndex], or -1 when there is none. */
internal fun rowIndexOf(rows: List<ListRow>, focusIndex: Int): Int =
    rows.indexOfFirst { focusIndex >= it.firstIndex && focusIndex < it.firstIndex + it.targets.size }

/**
 * Quick links, then every area with its categories as chips. Opening one pushes its
 * video list on top, so Back lands on the same item. [onOpenLongs] switches to the
 * menu's Longs screen instead.
 */
@Composable
fun CategoriesScreen(viewModel: AppViewModel, onOpen: (Screen) -> Unit, onOpenLongs: () -> Unit) {
    val screen = Screen.Categories
    Column(Modifier.fillMaxSize()) {
        ScreenHeading("Categories")
        LoadedScreen(load = { buildRows(viewModel.areas()) }) { rows, movesAtLoad ->
            CategoryList(
                rows = rows,
                initialFocus = remember { viewModel.focusIndex(screen) },
                movesBefore = movesAtLoad,
                onFocusChange = { viewModel.setFocusIndex(screen, it) },
                onOpen = { target -> if (target.screen == null) onOpenLongs() else onOpen(target.screen) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryList(
    rows: List<ListRow>,
    initialFocus: Int,
    movesBefore: Int,
    onFocusChange: (Int) -> Unit,
    onOpen: (Target) -> Unit,
) {
    val listState = rememberLazyListState()
    val focus = rememberListFocus(
        listState = listState,
        stopCount = rows.sumOf { it.targets.size },
        itemOf = { rowIndexOf(rows, it) },
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            rows.forEach { row ->
                item(key = row.key) {
                    val chip: @Composable (Int, Target) -> Unit = { offset, target ->
                        TargetChip(
                            target = target,
                            onClick = { onOpen(target) },
                            modifier = Modifier.focusStop(row.firstIndex + offset),
                        )
                    }
                    val spacing = Arrangement.spacedBy(10.dp)
                    if (row.wrapping) {
                        FlowRow(horizontalArrangement = spacing, verticalArrangement = spacing) {
                            row.targets.forEachIndexed { offset, target -> chip(offset, target) }
                        }
                    } else {
                        Row(horizontalArrangement = spacing) {
                            row.targets.forEachIndexed { offset, target -> chip(offset, target) }
                        }
                    }
                }
            }
        }
    }
}

/** A name with its count in the secondary color; white while focused, like the menu. */
@Composable
private fun TargetChip(target: Target, onClick: () -> Unit, modifier: Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(ChipShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = AppColors.Raised,
            contentColor = AppColors.Text,
            focusedContainerColor = AppColors.Text,
            focusedContentColor = AppColors.Background,
            pressedContainerColor = AppColors.Text,
            pressedContentColor = AppColors.Background,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = target.label, fontSize = 14.sp, maxLines = 1)
            if (target.count != null) {
                Text(
                    text = formatCount(target.count),
                    modifier = Modifier.padding(start = 8.dp),
                    color = LocalContentColor.current.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                )
            }
        }
    }
}
