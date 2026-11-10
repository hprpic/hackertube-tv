package biz.prpic.hackertubetv.paging

import biz.prpic.hackertubetv.data.UserLibrary
import biz.prpic.hackertubetv.nav.Screen
import org.junit.Assert.assertEquals
import org.junit.Test

class SavedIdsTest {
    @Test fun newestFirstPerScreen() {
        val later = UserLibrary().toggleLater("a").toggleLater("b")
        assertEquals(listOf("b", "a"), savedIds(later, Screen.WatchLater))

        assertEquals(listOf("y", "x"), savedIds(UserLibrary(seen = listOf("x", "y")), Screen.History))

        assertEquals(listOf("q", "p"), savedIds(UserLibrary(likes = listOf("p", "q")), Screen.Liked))

        val full = UserLibrary(likes = listOf("p"), seen = listOf("x"), later = listOf("a"))
        assertEquals(emptyList<String>(), savedIds(full, Screen.Home))
    }
}
