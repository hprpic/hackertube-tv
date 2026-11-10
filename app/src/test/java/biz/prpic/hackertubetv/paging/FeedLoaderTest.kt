package biz.prpic.hackertubetv.paging

import biz.prpic.hackertubetv.data.HackerTubeApi
import biz.prpic.hackertubetv.data.UserLibrary
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class FeedLoaderTest {
    private val server = MockWebServer()

    @Before fun setUp() {
        server.start()
    }

    @After fun tearDown() {
        server.close()
    }

    @Test fun readsLibraryOnEveryCall() = runTest {
        server.enqueue(MockResponse(body = """[{"vid":"a"},{"vid":"b"}]"""))
        server.enqueue(MockResponse(body = "[]"))
        val likes = (1..150).map { "l$it" }
        val dislikes = (1..150).map { "d$it" }
        var library = UserLibrary(likes = likes, dislikes = dislikes)
        val load = feedLoader(HackerTubeApi(baseUrl = server.url("/")), { library }, seed = "7")

        assertEquals(2, load(0).consumed)
        val first = server.takeRequest().url
        assertEquals(likes.takeLast(100), first.queryParameter("like")?.split(','))
        assertEquals(dislikes.takeLast(100), first.queryParameter("dislike")?.split(','))

        library = library.toggleLike("l200").markSeen("s1")
        assertEquals(Page(emptyList(), 0), load(2))
        val second = server.takeRequest().url
        assertEquals(likes.takeLast(99) + "l200", second.queryParameter("like")?.split(','))
        assertEquals("s1", second.queryParameter("seen"))
        assertEquals("7", second.queryParameter("seed"))
        assertEquals("2", second.queryParameter("offset"))
    }
}
