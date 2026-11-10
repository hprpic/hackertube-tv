package biz.prpic.hackertubetv.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import biz.prpic.hackertubetv.ui.VideoImage

internal val MosaicShape = RoundedCornerShape(8.dp)

/**
 * Up to four video images in a 2 x 2 block with the 16:9 shape of a video card. Each image
 * is decoded at [cellWidthPx] x [cellHeightPx], the size of one cell on the TV, so a small
 * mosaic does not hold large images in memory.
 */
@Composable
internal fun PlaylistMosaic(
    vids: List<String>,
    showFrames: Boolean,
    cellWidthPx: Int,
    cellHeightPx: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier.aspectRatio(16f / 9f).clip(MosaicShape)) {
        for (row in 0 until 2) {
            Row(Modifier.weight(1f)) {
                for (column in 0 until 2) {
                    val vid = vids.getOrNull(row * 2 + column)
                    val cell = Modifier.weight(1f).fillMaxSize()
                    if (vid == null) {
                        Box(cell)
                    } else {
                        VideoImage(vid, showFrames, cell, decodeWidth = cellWidthPx, decodeHeight = cellHeightPx)
                    }
                }
            }
        }
    }
}
