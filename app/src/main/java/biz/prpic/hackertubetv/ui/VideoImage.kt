package biz.prpic.hackertubetv.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.memory.MemoryCache
import coil3.network.HttpException
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.ImageRequest
import coil3.serviceLoaderEnabled
import java.io.IOException
import okhttp3.OkHttpClient

// A card shows its image at about 200 x 112 dp, which is 400 x 225 pixels on the TV.
// Decoding at that size instead of the full 480 x 360 file keeps memory use low.
private const val DECODE_WIDTH_PX = 400
private const val DECODE_HEIGHT_PX = 225

// Share of the app's memory the decoded images may keep; the TV has little RAM.
private const val MEMORY_CACHE_SHARE = 0.15

/**
 * The images to try for a video card, best first. A frame from the middle of the video
 * is preferred when frames are on; the uploader's thumbnail is the fallback.
 */
fun imageUrls(vid: String, showFrames: Boolean): List<String> {
    val thumbnail = "https://i.ytimg.com/vi/$vid/hqdefault.jpg"
    return if (showFrames) listOf("https://i.ytimg.com/vi/$vid/hq2.jpg", thumbnail) else listOf(thumbnail)
}

/**
 * Whether a failed image should be replaced by the next URL. Yes when the server answered
 * with an error status, as YouTube does for a video without frames, or sent something
 * that is not an image. No for a network failure: the same URL loads once the connection
 * is back, and moving on would leave the card on its last fallback for good.
 */
fun tryNextImage(error: Throwable): Boolean = when (error) {
    is HttpException -> true
    is IOException -> false
    // Coil reports an image it cannot decode as an IllegalStateException.
    else -> true
}

/**
 * The video's image, cropped to fill. When the server refuses one URL or it is not an
 * image, the next one is tried; after a network failure the card keeps its URL and loads
 * it again the next time it is composed. The decode size defaults to that of a video card;
 * smaller images such as playlist mosaic cells pass their own.
 */
@Composable
fun VideoImage(
    vid: String,
    showFrames: Boolean,
    modifier: Modifier = Modifier,
    decodeWidth: Int = DECODE_WIDTH_PX,
    decodeHeight: Int = DECODE_HEIGHT_PX,
) {
    val urls = remember(vid, showFrames) { imageUrls(vid, showFrames) }
    var attempt by remember(urls) { mutableIntStateOf(0) }
    val context = LocalPlatformContext.current
    val request = remember(context, urls, attempt, decodeWidth, decodeHeight) {
        ImageRequest.Builder(context)
            .data(urls[attempt])
            .size(decodeWidth, decodeHeight)
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = null,
        modifier = modifier.background(AppColors.Raised),
        contentScale = ContentScale.Crop,
        onError = { failed -> if (attempt < urls.lastIndex && tryNextImage(failed.result.throwable)) attempt++ },
    )
}

/**
 * The app's one image loader. It downloads through the same HTTP client as the API, so
 * both share connections and threads, and it keeps a small memory cache.
 */
fun appImageLoader(context: Context, client: OkHttpClient): ImageLoader =
    ImageLoader.Builder(context)
        // Components are registered here explicitly instead of being discovered at runtime.
        .serviceLoaderEnabled(false)
        .components { add(OkHttpNetworkFetcherFactory(callFactory = { client })) }
        .memoryCache { MemoryCache.Builder().maxSizePercent(context, MEMORY_CACHE_SHARE).build() }
        .build()
