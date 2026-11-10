package biz.prpic.hackertubetv.ui

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import biz.prpic.hackertubetv.nav.RailItem

private val ItemShape = RoundedCornerShape(10.dp)

/**
 * The left menu: every [RailItem] as an icon above a small label, in the site's order.
 * The item of the current screen is highlighted. [currentItemRequester] is attached to
 * that item, and focus coming into the menu from the content always lands on it. Up and
 * Down stay in the menu, so Down from the last item does not jump into the content.
 */
@Composable
fun NavRail(
    current: RailItem,
    currentItemRequester: FocusRequester,
    onSelect: (RailItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .focusProperties {
                onEnter = { currentItemRequester.requestFocus() }
                onExit = { keepUpAndDownInside() }
            }
            .focusGroup()
            .fillMaxHeight()
            .width(80.dp)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        RailItem.entries.forEach { item ->
            RailButton(
                item = item,
                selected = item == current,
                onClick = { onSelect(item) },
                modifier = if (item == current) Modifier.focusRequester(currentItemRequester) else Modifier,
            )
        }
    }
}

@Composable
private fun RailButton(item: RailItem, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.width(72.dp),
        shape = ClickableSurfaceDefaults.shape(ItemShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) AppColors.Raised else Color.Transparent,
            contentColor = if (selected) AppColors.Accent else AppColors.Secondary,
            focusedContainerColor = AppColors.Text,
            focusedContentColor = AppColors.Background,
            pressedContainerColor = AppColors.Text,
            pressedContentColor = AppColors.Background,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(imageVector = item.icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Text(text = item.label, modifier = Modifier.padding(top = 2.dp), fontSize = 10.sp, maxLines = 1)
        }
    }
}

private val RailItem.icon: ImageVector
    get() = when (this) {
        RailItem.SEARCH -> Icons.Filled.Search
        RailItem.HOME -> Icons.Filled.Home
        RailItem.LONGS -> Icons.Filled.Timer
        RailItem.LIKED -> Icons.Filled.ThumbUp
        RailItem.HISTORY -> Icons.Filled.History
        RailItem.WATCH_LATER -> Icons.Filled.WatchLater
        RailItem.CATEGORIES -> Icons.Filled.Category
        RailItem.PLAYLISTS -> Icons.AutoMirrored.Filled.PlaylistPlay
        RailItem.SETTINGS -> Icons.Filled.Settings
    }
