package biz.prpic.hackertubetv.data

import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

/** Number of videos the site asks for per page; the app pages the same way. */
const val PAGE_SIZE = 36

/** The site answered with a non-success HTTP status. */
class ApiException(val code: Int) : IOException("hackertube.org answered HTTP $code")

/** The site answered, but not with data the app can read (for example an HTML error page). */
class ApiFormatException(cause: Throwable) : IOException("hackertube.org sent data the app cannot read", cause)

/**
 * Client for the JSON endpoints the hackertube.org page script uses.
 *
 * Responses are kept in memory for a short while, so moving back and forth between
 * screens does not ask the site for the same data again.
 */
class HackerTubeApi(
    private val baseUrl: HttpUrl = "https://hackertube.org".toHttpUrl(),
    private val client: OkHttpClient = OkHttpClient(),
    clock: () -> Long = System::currentTimeMillis,
) {
    private val cache = ResponseCache(clock)

    /** The personalised Home feed. Empty lists are sent as empty parameters, as the site does. */
    suspend fun feed(
        likes: List<String>,
        dislikes: List<String>,
        seen: List<String>,
        seed: String,
        offset: Int,
        n: Int = PAGE_SIZE,
    ): List<Video> = get(
        "feed",
        "like" to likes.joinToString(","),
        "dislike" to dislikes.joinToString(","),
        "seen" to seen.joinToString(","),
        "seed" to seed,
        "offset" to offset.toString(),
        "n" to n.toString(),
    )

    /** A filtered video list, for example `dur=xlong`, `area=<id>` or `cat=<id>`. */
    suspend fun list(filters: Map<String, String>, offset: Int, n: Int = PAGE_SIZE): List<Video> =
        get("list", *filters.toList().toTypedArray(), "offset" to offset.toString(), "n" to n.toString())

    suspend fun browse(): List<Area> = get("browse")

    suspend fun playlists(): List<PlaylistSummary> = get("playlists")

    suspend fun playlist(slug: String): Playlist = get("playlist", "slug" to slug)

    /**
     * The videos for saved ids, in the order asked for. Ids the site no longer knows are
     * left out, so a saved list keeps working after a video disappears.
     */
    suspend fun docs(vids: List<String>): List<Video> {
        if (vids.isEmpty()) return emptyList()
        val byId = get<List<Video>>("docs", "vids" to vids.joinToString(",")).associateBy { it.vid }
        return vids.mapNotNull { byId[it] }
    }

    suspend fun search(query: String): SearchOutcome {
        if (query.isBlank()) return SearchOutcome.Results(SearchResponse())
        return try {
            SearchOutcome.Results(get("search", "q" to query))
        } catch (e: ApiException) {
            if (e.code == 429 || e.code == 503) SearchOutcome.Busy else throw e
        }
    }

    suspend fun config(): SiteConfig = get("config")

    private suspend inline fun <reified T> get(endpoint: String, vararg params: Pair<String, String>): T =
        fetch(serializer<T>(), endpoint, params)

    // Download and decoding both run off the caller's thread: a page of 36 videos is
    // about 100 KB of JSON, too much to parse on the TV's main thread.
    private suspend fun <T> fetch(
        deserializer: DeserializationStrategy<T>,
        endpoint: String,
        params: Array<out Pair<String, String>>,
    ): T = withContext(Dispatchers.IO) {
        val url = baseUrl.newBuilder()
            .addPathSegment("api")
            .addPathSegment(endpoint)
            .apply { params.forEach { (name, value) -> addQueryParameter(name, value) } }
            .build()
        val key = url.toString()
        val cached = cache.get(key)
        if (cached != null) return@withContext decode(deserializer, cached)
        val body = download(url)
        // Decoded before caching, so an unreadable answer is never served from memory.
        val value = decode(deserializer, body)
        cache.put(key, body)
        value
    }

    private fun download(url: HttpUrl): String =
        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) throw ApiException(response.code)
            response.body.string()
        }

    // Malformed JSON, or JSON of the wrong shape, surfaces as an IOException like any
    // other failure to get data from the site.
    private fun <T> decode(deserializer: DeserializationStrategy<T>, body: String): T =
        try {
            json.decodeFromString(deserializer, body)
        } catch (e: IllegalArgumentException) {
            throw ApiFormatException(e)
        }

    private companion object {
        val json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            explicitNulls = false
        }
    }
}

private const val CACHE_TTL_MILLIS = 10 * 60 * 1000L
private const val CACHE_MAX_ENTRIES = 50

/** Response bodies by full URL, least recently used dropped first, each valid for a fixed time. */
private class ResponseCache(private val clock: () -> Long) {
    private class Entry(val body: String, val fetchedAtMillis: Long)

    private val mutex = Mutex()
    private val entries = object : LinkedHashMap<String, Entry>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Entry>): Boolean =
            size > CACHE_MAX_ENTRIES
    }

    suspend fun get(key: String): String? = mutex.withLock {
        val entry = entries[key] ?: return@withLock null
        if (clock() - entry.fetchedAtMillis < CACHE_TTL_MILLIS) {
            entry.body
        } else {
            entries.remove(key)
            null
        }
    }

    suspend fun put(key: String, body: String) {
        mutex.withLock { entries[key] = Entry(body, clock()) }
    }
}
