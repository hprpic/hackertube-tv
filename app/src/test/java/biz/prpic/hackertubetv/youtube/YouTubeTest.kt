package biz.prpic.hackertubetv.youtube

import org.junit.Assert.assertEquals
import org.junit.Test

class YouTubeTest {
    @Test fun watchUriForOneVideo() {
        assertEquals("https://www.youtube.com/watch?v=abc", watchUri("abc"))
    }

    @Test fun playAllTakesTheFirstFiftyIds() {
        val ids = (1..60).map { "id$it" }
        val uri = playAllUri(ids)
        val prefix = "https://www.youtube.com/watch_videos?video_ids="
        assertEquals(prefix, uri.take(prefix.length))
        assertEquals(ids.take(50), uri.removePrefix(prefix).split(","))
    }
}
