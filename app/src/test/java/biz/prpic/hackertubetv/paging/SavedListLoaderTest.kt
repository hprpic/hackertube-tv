package biz.prpic.hackertubetv.paging

import biz.prpic.hackertubetv.data.HackerTubeApi
import kotlinx.coroutines.test.runTest
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SavedListLoaderTest {
    private val server = MockWebServer()

    @Before fun setUp() {
        // Answers every docs request with exactly the ids it asked for.
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val vids = request.url.queryParameter("vids").orEmpty().split(',')
                return MockResponse(body = vids.joinToString(",", "[", "]") { """{"vid":"$it"}""" })
            }
        }
        server.start()
    }

    @After fun tearDown() {
        server.close()
    }

    @Test fun requestsChunksOf36() = runTest {
        val ids = (1..80).map { "id$it" }
        val load = savedListLoader(HackerTubeApi(baseUrl = server.url("/")), ids)

        for ((offset, chunk) in listOf(0 to ids.subList(0, 36), 36 to ids.subList(36, 72), 72 to ids.subList(72, 80))) {
            val page = load(offset)
            assertEquals(chunk, server.takeRequest().url.queryParameter("vids")?.split(','))
            assertEquals(chunk, page.items.map { it.vid })
            assertEquals(chunk.size, page.consumed)
        }

        assertEquals(Page(emptyList(), 0), load(80))
        assertEquals(3, server.requestCount)
    }
}
