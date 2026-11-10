package biz.prpic.hackertubetv.paging

import biz.prpic.hackertubetv.data.HackerTubeApi
import biz.prpic.hackertubetv.data.PAGE_SIZE
import biz.prpic.hackertubetv.data.UserLibrary
import biz.prpic.hackertubetv.nav.Screen

/**
 * The Home feed. The library is read again for every page, so a like or a newly watched
 * video already shapes the next page.
 */
fun feedLoader(api: HackerTubeApi, library: () -> UserLibrary, seed: String): suspend (Int) -> Page =
    { offset ->
        val current = library()
        val items = api.feed(current.feedLikes(), current.feedDislikes(), current.feedSeen(), seed, offset)
        Page(items, items.size)
    }

/** A filtered list such as Longs, an area or a category. */
fun listLoader(api: HackerTubeApi, filters: Map<String, String>): suspend (Int) -> Page =
    { offset ->
        val items = api.list(filters, offset)
        Page(items, items.size)
    }

/**
 * Liked, Watch later or History: the saved ids are fetched a page at a time. The offset
 * moves by the number of ids asked for, not by the videos returned, so ids the site no
 * longer knows are skipped without stalling or repeating.
 */
fun savedListLoader(api: HackerTubeApi, idsNewestFirst: List<String>): suspend (Int) -> Page =
    { offset ->
        val slice = idsNewestFirst.drop(offset).take(PAGE_SIZE)
        if (slice.isEmpty()) Page(emptyList(), 0) else Page(api.docs(slice), slice.size)
    }

/**
 * The saved ids a personal screen lists, newest first: Liked from the likes, History from
 * the opened videos, Watch later from the saved ones. Every other screen has none.
 */
fun savedIds(library: UserLibrary, screen: Screen): List<String> = when (screen) {
    Screen.Liked -> library.likes.reversed()
    Screen.History -> library.seen.reversed()
    Screen.WatchLater -> library.later.reversed()
    else -> emptyList()
}
