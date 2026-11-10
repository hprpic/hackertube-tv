package biz.prpic.hackertubetv.ui

import coil3.network.HttpException
import coil3.network.NetworkResponse
import java.net.UnknownHostException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoImageUrlsTest {
    @Test fun framesFirstThenThumbnail() {
        assertEquals(
            listOf("https://i.ytimg.com/vi/abc/hq2.jpg", "https://i.ytimg.com/vi/abc/hqdefault.jpg"),
            imageUrls("abc", showFrames = true),
        )
    }

    @Test fun thumbnailOnlyWhenFramesAreOff() {
        assertEquals(listOf("https://i.ytimg.com/vi/abc/hqdefault.jpg"), imageUrls("abc", showFrames = false))
    }

    @Test fun nextImageOnlyWhenTheServerRefusedOrTheImageIsUnreadable() {
        assertTrue(tryNextImage(HttpException(NetworkResponse(code = 404))))
        assertTrue(tryNextImage(IllegalStateException("BitmapFactory returned a null bitmap.")))
        assertFalse(tryNextImage(UnknownHostException("i.ytimg.com")))
    }
}
