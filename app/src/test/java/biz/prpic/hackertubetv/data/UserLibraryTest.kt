package biz.prpic.hackertubetv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserLibraryTest {
    @Test fun likeAndDislikeAreExclusive() {
        val liked = UserLibrary(dislikes = listOf("a")).toggleLike("a")
        assertEquals(listOf("a"), liked.likes)
        assertEquals(emptyList<String>(), liked.dislikes)
        assertTrue(liked.isLiked("a"))
        assertFalse(liked.isDisliked("a"))
        assertEquals(emptyList<String>(), liked.toggleLike("a").likes)

        val disliked = UserLibrary(likes = listOf("a")).toggleDislike("a")
        assertEquals(listOf("a"), disliked.dislikes)
        assertEquals(emptyList<String>(), disliked.likes)
        assertTrue(disliked.isDisliked("a"))
        assertFalse(disliked.isLiked("a"))
        assertEquals(emptyList<String>(), disliked.toggleDislike("a").dislikes)
    }

    @Test fun markSeenMovesToEndAndCaps() {
        assertEquals(listOf("b", "a"), UserLibrary(seen = listOf("a", "b")).markSeen("a").seen)

        val full = (1..205).fold(UserLibrary()) { library, i -> library.markSeen("v$i") }
        assertEquals(SEEN_LIMIT, full.seen.size)
        assertEquals((6..205).map { "v$it" }, full.seen)
    }

    @Test fun clearHistoryEmptiesSeenOnly() {
        val library = UserLibrary(
            likes = listOf("l"), dislikes = listOf("d"), seen = listOf("s"), later = listOf("w"), showFrames = false,
        )
        assertEquals(library.copy(seen = emptyList()), library.clearHistory())
    }

    @Test fun toggleLaterAddsAndRemoves() {
        val library = UserLibrary().toggleLater("a").toggleLater("b")
        assertEquals(listOf("a", "b"), library.later)
        assertTrue(library.isLater("a"))

        val removed = library.toggleLater("a")
        assertEquals(listOf("b"), removed.later)
        assertFalse(removed.isLater("a"))
    }

    @Test fun feedParamsAreCapped() {
        val likes = (1..150).map { "l$it" }
        val dislikes = (1..150).map { "d$it" }
        val seen = (1..200).map { "s$it" }
        val library = UserLibrary(likes = likes, dislikes = dislikes, seen = seen)

        assertEquals(FEED_VOTE_LIMIT, library.feedLikes().size)
        assertEquals(likes.takeLast(100), library.feedLikes())
        assertEquals(FEED_VOTE_LIMIT, library.feedDislikes().size)
        assertEquals(dislikes.takeLast(100), library.feedDislikes())
        assertEquals(seen, library.feedSeen())
    }

    @Test fun withShowFramesSetsTheSwitch() {
        assertFalse(UserLibrary().withShowFrames(false).showFrames)
        assertTrue(UserLibrary(showFrames = false).withShowFrames(true).showFrames)
    }
}
