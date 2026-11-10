package biz.prpic.hackertubetv.paging

import biz.prpic.hackertubetv.data.HackerTubeApi
import biz.prpic.hackertubetv.data.SearchOutcome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What a finished search has to tell the viewer besides the videos themselves. */
enum class SearchNote { NONE, BUSY, EMPTY, WEAK }

/** The note for an answer. No results is "empty" whether or not the site called them weak. */
fun searchNote(outcome: SearchOutcome): SearchNote = when (outcome) {
    SearchOutcome.Busy -> SearchNote.BUSY
    is SearchOutcome.Results -> when {
        outcome.response.results.isEmpty() -> SearchNote.EMPTY
        outcome.response.weak -> SearchNote.WEAK
        else -> SearchNote.NONE
    }
}

/** The line shown for [note], or null when there is nothing to say. */
fun searchNoteText(note: SearchNote, query: String): String? = when (note) {
    SearchNote.NONE -> null
    SearchNote.BUSY -> "Lots of searches right now. Give it a minute and try again."
    SearchNote.EMPTY -> "No videos match “$query”."
    SearchNote.WEAK -> "No strong matches for “$query”. Here are the closest videos."
}

/**
 * One submitted search. The site answers a query with all its results at once, so the
 * pager has a single page: the first load asks the site, any later one finds the end.
 * Retrying after a failure repeats the same query.
 */
class SearchSession(scope: CoroutineScope, api: HackerTubeApi, val query: String) {
    private val mutableNote = MutableStateFlow(SearchNote.NONE)
    val note: StateFlow<SearchNote> = mutableNote.asStateFlow()

    val pager = VideoPager(scope) { offset ->
        if (offset > 0) {
            Page(emptyList(), 0)
        } else {
            when (val outcome = api.search(query)) {
                SearchOutcome.Busy -> {
                    mutableNote.value = searchNote(outcome)
                    Page(emptyList(), 0)
                }
                is SearchOutcome.Results -> {
                    mutableNote.value = searchNote(outcome)
                    val videos = outcome.response.results
                    Page(videos, videos.size)
                }
            }
        }
    }
}
