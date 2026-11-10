package biz.prpic.hackertubetv.paging

import biz.prpic.hackertubetv.data.PAGE_SIZE
import biz.prpic.hackertubetv.data.Video
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// The pager runs in the test scope itself, so advanceUntilIdle drives its loads. Every
// load in these tests finishes, so runTest does not wait on anything left behind.
@OptIn(ExperimentalCoroutinesApi::class)
class VideoPagerTest {
    private val offsets = mutableListOf<Int>()

    private fun videos(from: Int, count: Int) = (from until from + count).map { Video(vid = "v$it") }

    private fun fullPage(offset: Int) = Page(videos(offset, PAGE_SIZE), PAGE_SIZE)

    @Test fun startLoadsFirstPage() = runTest {
        val pager = VideoPager(this) { offset -> offsets += offset; fullPage(offset) }
        pager.start()
        advanceUntilIdle()
        assertEquals(listOf(0), offsets)
        assertEquals(36, pager.state.value.items.size)
        assertFalse(pager.state.value.loading)
    }

    @Test fun prefetchesNearEnd() = runTest {
        val pager = VideoPager(this) { offset -> offsets += offset; fullPage(offset) }
        pager.start()
        advanceUntilIdle()

        pager.onFocused(27)
        advanceUntilIdle()
        assertEquals(listOf(0), offsets)

        pager.onFocused(28)
        advanceUntilIdle()
        assertEquals(listOf(0, 36), offsets)
    }

    @Test fun singleLoadAtATime() = runTest {
        val release = CompletableDeferred<Unit>()
        val pager = VideoPager(this) { offset ->
            offsets += offset
            if (offset > 0) release.await()
            fullPage(offset)
        }
        pager.start()
        advanceUntilIdle()

        pager.onFocused(30)
        pager.onFocused(30)
        advanceUntilIdle()
        assertEquals(listOf(0, 36), offsets)
        assertTrue(pager.state.value.loading)

        release.complete(Unit)
        advanceUntilIdle()
        assertEquals(72, pager.state.value.items.size)
        assertFalse(pager.state.value.loading)
    }

    @Test fun emptyPageEnds() = runTest {
        val pager = VideoPager(this) { offset ->
            offsets += offset
            if (offset == 0) fullPage(offset) else Page(emptyList(), 0)
        }
        pager.start()
        advanceUntilIdle()
        pager.onFocused(35)
        advanceUntilIdle()
        assertTrue(pager.state.value.endReached)
        assertEquals(listOf(0, 36), offsets)

        pager.onFocused(35)
        advanceUntilIdle()
        assertEquals(listOf(0, 36), offsets)
    }

    @Test fun errorKeepsItemsAndRetryResumes() = runTest {
        var offline = true
        val pager = VideoPager(this) { offset ->
            offsets += offset
            if (offset > 0 && offline) throw IOException("offline")
            fullPage(offset)
        }
        pager.start()
        advanceUntilIdle()
        pager.onFocused(35)
        advanceUntilIdle()
        assertEquals(36, pager.state.value.items.size)
        assertTrue(pager.state.value.error)
        assertFalse(pager.state.value.loading)

        // Moving focus does not retry on its own; only Retry does.
        pager.onFocused(35)
        advanceUntilIdle()
        assertEquals(listOf(0, 36), offsets)

        offline = false
        pager.retry()
        advanceUntilIdle()
        assertEquals(listOf(0, 36, 36), offsets)
        assertEquals(72, pager.state.value.items.size)
        assertFalse(pager.state.value.error)
    }

    @Test fun offsetAdvancesByConsumedNotReturned() = runTest {
        val pager = VideoPager(this) { offset ->
            offsets += offset
            if (offset == 0) Page(videos(0, 34), consumed = 36) else fullPage(offset)
        }
        pager.start()
        advanceUntilIdle()
        assertEquals(34, pager.state.value.items.size)

        pager.onFocused(33)
        advanceUntilIdle()
        assertEquals(listOf(0, 36), offsets)
    }

    @Test fun dropsDuplicateVids() = runTest {
        val pager = VideoPager(this) { offset ->
            offsets += offset
            if (offset == 0) fullPage(0) else Page(listOf(Video(vid = "v5")) + videos(36, 35), PAGE_SIZE)
        }
        pager.start()
        advanceUntilIdle()
        pager.onFocused(35)
        advanceUntilIdle()

        val vids = pager.state.value.items.map { it.vid }
        assertEquals(71, vids.size)
        assertEquals(vids.distinct(), vids)
    }

    @Test fun pageWithNothingNewThenEndEndsWithoutFocus() = runTest {
        val pager = VideoPager(this) { offset ->
            offsets += offset
            if (offset == 0) Page(emptyList(), consumed = 3) else Page(emptyList(), 0)
        }
        pager.start()
        advanceUntilIdle()
        assertEquals(listOf(0, 3), offsets)
        assertTrue(pager.state.value.endReached)
        assertFalse(pager.state.value.loading)
        assertEquals(emptyList<Video>(), pager.state.value.items)
    }

    @Test fun pageWithNothingNewLoadsTheNextOneByItself() = runTest {
        val pager = VideoPager(this) { offset ->
            offsets += offset
            when (offset) {
                36 -> Page(videos(0, 10), PAGE_SIZE)
                else -> fullPage(offset)
            }
        }
        pager.start()
        advanceUntilIdle()
        pager.onFocused(35)
        advanceUntilIdle()
        assertEquals(listOf(0, 36, 72), offsets)
        assertEquals(72, pager.state.value.items.size)
        assertFalse(pager.state.value.loading)
    }

    @Test fun stopsAfterFivePagesWithNothingNew() = runTest {
        val pager = VideoPager(this) { offset ->
            offsets += offset
            // The first page shows one card; the six pages after it only repeat that card.
            if (offset <= 6 * PAGE_SIZE) Page(videos(0, 1), PAGE_SIZE) else fullPage(offset)
        }
        pager.start()
        advanceUntilIdle()
        assertEquals(listOf(0), offsets)

        pager.onFocused(0)
        advanceUntilIdle()
        assertEquals(listOf(0, 36, 72, 108, 144, 180), offsets)
        assertTrue(pager.state.value.endReached)
        assertFalse(pager.state.value.loading)
        assertEquals(1, pager.state.value.items.size)
    }

    @Test fun loadUntilStopsOnError() = runTest {
        val pager = VideoPager(this) { offset ->
            offsets += offset
            throw IOException("offline")
        }
        pager.loadUntil(80)
        advanceUntilIdle()
        assertEquals(listOf(0), offsets)
        assertTrue(pager.state.value.error)
        assertFalse(pager.state.value.loading)
    }

    @Test fun loadUntilStopsAtTheEnd() = runTest {
        val pager = VideoPager(this) { offset ->
            offsets += offset
            if (offset == 0) fullPage(0) else Page(emptyList(), 0)
        }
        pager.loadUntil(80)
        advanceUntilIdle()
        assertEquals(listOf(0, 36), offsets)
        assertTrue(pager.state.value.endReached)
        assertEquals(36, pager.state.value.items.size)
    }

    @Test fun loadUntilFetchesEnoughPages() = runTest {
        val pager = VideoPager(this) { offset -> offsets += offset; fullPage(offset) }
        pager.loadUntil(80)
        advanceUntilIdle()
        assertEquals(listOf(0, 36, 72), offsets)
        assertEquals(108, pager.state.value.items.size)
    }
}
