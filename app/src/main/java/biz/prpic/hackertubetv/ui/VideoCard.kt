package biz.prpic.hackertubetv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Text
import biz.prpic.hackertubetv.data.Video
import biz.prpic.hackertubetv.format.formatDuration
import biz.prpic.hackertubetv.format.videoSubline

private val ImageShape = RoundedCornerShape(8.dp)
private const val FOCUSED_SCALE = 1.04f

/**
 * One video in a grid: the 16:9 image with the duration in its corner, then the title,
 * the channel and the "views • age" line. When focused, the whole card grows slightly
 * and the image gets a white frame, like a focused video on YouTube.
 */
@Composable
fun VideoCard(
    video: Video,
    showFrames: Boolean,
    nowMillis: Long,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    Card(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        colors = CardDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = AppColors.Text,
            focusedContainerColor = Color.Transparent,
            focusedContentColor = AppColors.Text,
            pressedContainerColor = Color.Transparent,
            pressedContentColor = AppColors.Text,
        ),
        scale = CardDefaults.scale(focusedScale = FOCUSED_SCALE),
        // The frame is drawn around the image only, not around the text below it.
        border = CardDefaults.border(focusedBorder = Border.None),
        interactionSource = interactionSource,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(ImageShape)
                .then(if (focused) Modifier.border(3.dp, AppColors.FocusFrame, ImageShape) else Modifier),
        ) {
            VideoImage(video.vid, showFrames, Modifier.fillMaxSize())
            val duration = formatDuration(video.durationSeconds)
            if (duration.isNotEmpty()) {
                Text(
                    text = duration,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .background(AppColors.Badge, RoundedCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                    color = AppColors.Text,
                    fontSize = 11.sp,
                )
            }
        }
        Text(
            text = video.title,
            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 18.sp,
            // Two lines always, so the cards in a row line up.
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        SecondaryLine(video.channel)
        SecondaryLine(videoSubline(video.views, video.published, nowMillis))
    }
}

@Composable
private fun SecondaryLine(text: String) {
    Text(
        text = text,
        color = AppColors.Secondary,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
