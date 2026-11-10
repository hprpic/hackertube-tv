package biz.prpic.hackertubetv.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Switch
import androidx.tv.material3.Text
import biz.prpic.hackertubetv.data.UserLibrary
import biz.prpic.hackertubetv.ui.AppColors

private val RowShape = RoundedCornerShape(10.dp)

/**
 * The app's one setting: video frames instead of thumbnails. The row is a single focus
 * stop and OK flips the switch; cards on every screen read the setting from [library], so
 * they change at once.
 */
@Composable
fun SettingsScreen(library: UserLibrary, onToggle: () -> Unit) {
    val rowRequester = remember { FocusRequester() }
    // Like every screen, Settings takes focus when it appears.
    LaunchedEffect(Unit) { rowRequester.requestFocus() }
    Column(Modifier.fillMaxSize()) {
        ScreenHeading("Settings")
        Surface(
            onClick = onToggle,
            modifier = Modifier
                .focusRequester(rowRequester)
                .padding(start = 8.dp, end = 32.dp)
                .widthIn(max = 900.dp)
                .fillMaxWidth(),
            shape = ClickableSurfaceDefaults.shape(RowShape),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = AppColors.Raised,
                contentColor = AppColors.Text,
                focusedContainerColor = AppColors.Raised,
                focusedContentColor = AppColors.Text,
                pressedContainerColor = AppColors.Raised,
                pressedContentColor = AppColors.Text,
            ),
            border = ClickableSurfaceDefaults.border(
                focusedBorder = Border(
                    BorderStroke(3.dp, AppColors.FocusFrame),
                    shape = RowShape,
                ),
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Show video frames instead of thumbnails",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "YouTube thumbnails are designed by uploaders to grab your attention: big faces, " +
                            "arrows, shouting text. With this on, every video is shown with a frame from the " +
                            "middle of the video instead, so you see what it actually looks like. Turn it off " +
                            "to see the original thumbnails.",
                        modifier = Modifier.padding(top = 6.dp),
                        color = AppColors.Secondary,
                        fontSize = 14.sp,
                    )
                }
                Switch(
                    checked = library.showFrames,
                    onCheckedChange = null,
                    modifier = Modifier.padding(start = 24.dp),
                )
            }
        }
        Text(
            text = "Videos and data from hackertube.org",
            modifier = Modifier.padding(start = 8.dp, top = 20.dp),
            color = AppColors.Secondary,
            fontSize = 14.sp,
        )
    }
}
