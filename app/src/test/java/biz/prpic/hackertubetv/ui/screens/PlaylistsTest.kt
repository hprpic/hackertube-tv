package biz.prpic.hackertubetv.ui.screens

import biz.prpic.hackertubetv.data.Playlist
import biz.prpic.hackertubetv.data.PlaylistSummary
import biz.prpic.hackertubetv.data.Video
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistsTest {
    private fun summary(slug: String, tier: String = "deep") = PlaylistSummary(slug, slug, tier)

    @Test
    fun startTierGoesFirstAndOthersAreDeep() {
        val (start, deep) = splitPlaylists(listOf(summary("a", "start"), summary("b"), summary("c", "other"), summary("d", "start")))
        assertEquals(listOf("a", "d"), start.map { it.slug })
        assertEquals(listOf("b", "c"), deep.map { it.slug })
    }

    @Test
    fun tileLineShowsCountAndHours() {
        assertEquals("12 videos · 2 hr 5 min", playlistTileLine(PlaylistSummary("s", "t", count = 12, minutes = 125)))
    }

    @Test
    fun metaLineCountsTheVideosOnThePage() {
        val playlist = Playlist("t", minutes = 45, videos = listOf(Video("a"), Video("b")))
        assertEquals("Curated by Claude · 2 videos · 45 min", playlistMetaLine(playlist))
    }

    @Test
    fun entriesNumberTilesAcrossBothSections() {
        val playlists = listOf(summary("s1", "start")) + (1..5).map { summary("d$it") } + listOf(summary("s2", "start"))
        val entries = buildEntries(playlists)
        assertEquals(listOf("start", "start:0", "deep", "deep:0", "deep:1"), entries.map { it.key })
        val tiles = entries.filterIsInstance<Entry.Tiles>()
        assertEquals(listOf(0, 2, 6), tiles.map { it.firstIndex })
        assertEquals(listOf(listOf("s1", "s2"), listOf("d1", "d2", "d3", "d4"), listOf("d5")), tiles.map { row -> row.tiles.map { it.slug } })
    }

    @Test
    fun entryIndexOfSkipsSectionHeadings() {
        val playlists = listOf(summary("s1", "start")) + (1..5).map { summary("d$it") }
        val entries = buildEntries(playlists)
        assertEquals(listOf(1, 3, 3, 3, 3, 4), (0 until 6).map { entryIndexOf(entries, it) })
        assertEquals(-1, entryIndexOf(entries, 6))
        assertEquals(-1, entryIndexOf(buildEntries(emptyList()), 0))
    }
}
