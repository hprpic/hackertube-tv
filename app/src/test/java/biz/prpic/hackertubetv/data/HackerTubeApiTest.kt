package biz.prpic.hackertubetv.data

import java.io.IOException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class HackerTubeApiTest {
    private val server = MockWebServer()
    private var nowMillis = 0L
    private lateinit var api: HackerTubeApi

    @Before fun setUp() {
        server.start()
        api = HackerTubeApi(baseUrl = server.url("/"), clock = { nowMillis })
    }

    @After fun tearDown() {
        server.close()
    }

    private fun fixture(name: String): String =
        checkNotNull(javaClass.getResource("/fixtures/$name.json")) { "missing fixture $name" }.readText()

    private fun enqueue(body: String = "", code: Int = 200) {
        server.enqueue(MockResponse(code = code, body = body))
    }

    @Test fun feedBuildsSiteQuery() = runTest {
        enqueue(fixture("feed"))
        val videos = api.feed(
            likes = listOf("a", "b"), dislikes = listOf("c"), seen = listOf("d"), seed = "42", offset = 36,
        )
        val url = server.takeRequest().url
        assertEquals("/api/feed", url.encodedPath)
        assertEquals("a,b", url.queryParameter("like"))
        assertEquals("c", url.queryParameter("dislike"))
        assertEquals("d", url.queryParameter("seen"))
        assertEquals("42", url.queryParameter("seed"))
        assertEquals("36", url.queryParameter("offset"))
        assertEquals("36", url.queryParameter("n"))
        assertEquals(Json.parseToJsonElement(fixture("feed")).jsonArray.size, videos.size)
    }

    @Test fun listPassesFilters() = runTest {
        enqueue(fixture("list_longs"))
        api.list(mapOf("dur" to "xlong"), 0)
        assertEquals("/api/list?dur=xlong&offset=0&n=36", server.takeRequest().target)
    }

    @Test fun browseParsesAreas() = runTest {
        enqueue(fixture("browse"))
        val areas = api.browse()
        assertEquals("/api/browse", server.takeRequest().target)
        assertTrue(areas.first().categories.isNotEmpty())
    }

    @Test fun playlistsAndPlaylistParse() = runTest {
        enqueue(fixture("playlists"))
        enqueue(fixture("playlist"))
        val summaries = api.playlists()
        val playlist = api.playlist(summaries.first().slug)
        assertTrue(summaries.first().cover.isNotEmpty())
        assertTrue(playlist.videos.isNotEmpty())
        assertEquals("/api/playlists", server.takeRequest().target)
        assertEquals("/api/playlist?slug=${summaries.first().slug}", server.takeRequest().target)
    }

    @Test fun docsKeepsRequestOrderAndDropsMissing() = runTest {
        enqueue(fixture("docs"))
        val videos = api.docs(listOf("gH6iJ8kL0mN", "doesNotExist1", "aB3dE5fG7hI"))
        assertEquals(listOf("gH6iJ8kL0mN", "aB3dE5fG7hI"), videos.map { it.vid })
        assertEquals(
            "gH6iJ8kL0mN,doesNotExist1,aB3dE5fG7hI",
            server.takeRequest().url.queryParameter("vids"),
        )

        assertEquals(emptyList<Video>(), api.docs(emptyList()))
        assertEquals(1, server.requestCount)
    }

    @Test fun searchBusyOn429And503() = runTest {
        enqueue(code = 429)
        enqueue(code = 503)
        enqueue(fixture("search"))
        enqueue("""{"results":[{"vid":"x"}],"weak":true}""")
        assertEquals(SearchOutcome.Busy, api.search("rust"))
        assertEquals(SearchOutcome.Busy, api.search("rust"))

        val strong = api.search("rust") as SearchOutcome.Results
        assertTrue(strong.response.results.isNotEmpty())
        assertEquals(false, strong.response.weak)

        val weak = api.search("zzz") as SearchOutcome.Results
        assertEquals(true, weak.response.weak)
        assertEquals("/api/search?q=rust", server.takeRequest().target)
    }

    @Test fun blankSearchMakesNoRequest() = runTest {
        assertEquals(SearchOutcome.Results(SearchResponse()), api.search("  "))
        assertEquals(0, server.requestCount)
    }

    @Test fun parsesItemWithMissingFields() = runTest {
        enqueue("""[{"vid":"x","future_field":1}]""")
        assertEquals(listOf(Video(vid = "x")), api.list(emptyMap(), 0))
    }

    @Test fun nullFieldsFallBackToDefaults() = runTest {
        enqueue("""[{"vid":"x","title":null,"channel":null,"views":null,"duration_s":null}]""")
        assertEquals(listOf(Video(vid = "x")), api.list(emptyMap(), 0))
    }

    @Test fun unreadableBodyThrowsIOExceptionAndIsNotCached() = runTest {
        enqueue("<html>down</html>")
        enqueue("""{"results":[]}""")
        enqueue("[]")
        for (attempt in 1..2) {
            try {
                api.list(emptyMap(), 0)
                fail("expected an IOException on attempt $attempt")
            } catch (e: IOException) {
                assertTrue(e is ApiFormatException)
            }
        }
        assertEquals(emptyList<Video>(), api.list(emptyMap(), 0))
        assertEquals(3, server.requestCount)
    }

    @Test fun serverErrorThrowsApiException() = runTest {
        enqueue(code = 500)
        try {
            api.list(emptyMap(), 0)
            fail("expected ApiException")
        } catch (e: ApiException) {
            assertEquals(500, e.code)
        }
    }

    @Test fun cacheServesRepeatWithinTtl() = runTest {
        enqueue(fixture("browse"))
        enqueue(fixture("browse"))
        nowMillis = 0
        api.browse()
        nowMillis = 9 * MINUTE
        api.browse()
        assertEquals(1, server.requestCount)

        nowMillis = 11 * MINUTE
        api.browse()
        assertEquals(2, server.requestCount)
    }

    @Test fun configParses() = runTest {
        enqueue(fixture("config"))
        assertEquals(SiteConfig(framesSource = "youtube"), api.config())
    }

    private companion object {
        const val MINUTE = 60_000L
    }
}
