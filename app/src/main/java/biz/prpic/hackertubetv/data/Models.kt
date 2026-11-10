package biz.prpic.hackertubetv.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The hackertube.org API is not a published contract, so every field the app can live
// without has a default: a missing key or a null value parses instead of failing the
// whole response. Fields the app does not use are ignored by the JSON reader.

@Serializable
data class Video(
    val vid: String,
    val title: String = "",
    val channel: String = "",
    /** ISO timestamp such as "2025-02-10T14:03:41Z". */
    val published: String? = null,
    @SerialName("duration_s") val durationSeconds: Int? = null,
    val views: Long? = null,
    val summary: String = "",
    @SerialName("why_hn") val whyHn: String = "",
    /** Only on videos inside a playlist: why the video has its place in the list. */
    val note: String? = null,
)

@Serializable
data class Category(val id: String, val name: String, val count: Int = 0)

@Serializable
data class Area(
    val id: String,
    val name: String,
    val count: Int = 0,
    val categories: List<Category> = emptyList(),
)

@Serializable
data class PlaylistSummary(
    val slug: String,
    val title: String,
    /** "start" or "deep"; the site files anything else under deep dives. */
    val tier: String = "deep",
    /** Video ids for the cover mosaic. */
    val cover: List<String> = emptyList(),
    val count: Int = 0,
    val minutes: Int = 0,
)

@Serializable
data class Playlist(
    val title: String,
    val intro: String = "",
    val minutes: Int = 0,
    val videos: List<Video> = emptyList(),
)

@Serializable
data class SearchResponse(
    val results: List<Video> = emptyList(),
    /** True when nothing matched well and the results are only the closest videos. */
    val weak: Boolean = false,
)

sealed interface SearchOutcome {
    data class Results(val response: SearchResponse) : SearchOutcome

    /** The site answered 429 or 503: too many searches right now. */
    data object Busy : SearchOutcome
}

@Serializable
data class SiteConfig(@SerialName("frames_source") val framesSource: String = "youtube")
