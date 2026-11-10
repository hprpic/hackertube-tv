package biz.prpic.hackertubetv.paging

import biz.prpic.hackertubetv.data.SearchOutcome
import biz.prpic.hackertubetv.data.SearchResponse
import biz.prpic.hackertubetv.data.Video
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchNoteTest {
    private fun results(count: Int, weak: Boolean) =
        SearchOutcome.Results(SearchResponse(List(count) { Video(vid = "v$it", title = "t") }, weak))

    @Test
    fun busyAnswerIsBusy() {
        assertEquals(SearchNote.BUSY, searchNote(SearchOutcome.Busy))
    }

    @Test
    fun noResultsIsEmptyWhetherOrNotWeak() {
        assertEquals(SearchNote.EMPTY, searchNote(results(0, weak = false)))
        assertEquals(SearchNote.EMPTY, searchNote(results(0, weak = true)))
    }

    @Test
    fun weakResultsAreWeak() {
        assertEquals(SearchNote.WEAK, searchNote(results(2, weak = true)))
    }

    @Test
    fun strongResultsNeedNoNote() {
        assertEquals(SearchNote.NONE, searchNote(results(2, weak = false)))
        assertNull(searchNoteText(SearchNote.NONE, "x"))
    }

    @Test
    fun textsUseCurlyQuotesAroundTheQuery() {
        assertEquals("No videos match “rust”.", searchNoteText(SearchNote.EMPTY, "rust"))
        assertEquals(
            "No strong matches for “rust”. Here are the closest videos.",
            searchNoteText(SearchNote.WEAK, "rust"),
        )
        assertEquals(
            "Lots of searches right now. Give it a minute and try again.",
            searchNoteText(SearchNote.BUSY, "rust"),
        )
    }
}
