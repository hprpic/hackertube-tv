package biz.prpic.hackertubetv.data

/** History keeps only this many of the most recently opened videos. */
const val SEEN_LIMIT = 200

/** A feed request carries at most this many of the newest likes, and as many dislikes. */
const val FEED_VOTE_LIMIT = 100

/**
 * What the viewer saved on this TV. Every list holds video ids, oldest first and newest
 * last. The class is immutable: each change returns a new library.
 */
data class UserLibrary(
    val likes: List<String> = emptyList(),
    val dislikes: List<String> = emptyList(),
    val seen: List<String> = emptyList(),
    val later: List<String> = emptyList(),
    /** Show a frame from the middle of each video instead of the uploader's thumbnail. */
    val showFrames: Boolean = true,
) {
    fun isLiked(vid: String): Boolean = vid in likes

    fun isDisliked(vid: String): Boolean = vid in dislikes

    fun isLater(vid: String): Boolean = vid in later

    /** Liking a video takes it off the "not interested" list, and the other way round. */
    fun toggleLike(vid: String): UserLibrary =
        if (isLiked(vid)) copy(likes = likes - vid) else copy(likes = likes + vid, dislikes = dislikes - vid)

    fun toggleDislike(vid: String): UserLibrary =
        if (isDisliked(vid)) copy(dislikes = dislikes - vid) else copy(dislikes = dislikes + vid, likes = likes - vid)

    fun toggleLater(vid: String): UserLibrary =
        if (isLater(vid)) copy(later = later - vid) else copy(later = later + vid)

    /** Moves the video to the newest end of the history and drops the oldest past the limit. */
    fun markSeen(vid: String): UserLibrary = copy(seen = (seen - vid + vid).takeLast(SEEN_LIMIT))

    fun clearHistory(): UserLibrary = copy(seen = emptyList())

    fun withShowFrames(on: Boolean): UserLibrary = copy(showFrames = on)

    fun feedLikes(): List<String> = likes.takeLast(FEED_VOTE_LIMIT)

    fun feedDislikes(): List<String> = dislikes.takeLast(FEED_VOTE_LIMIT)

    fun feedSeen(): List<String> = seen
}
