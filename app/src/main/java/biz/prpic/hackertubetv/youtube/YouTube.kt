package biz.prpic.hackertubetv.youtube

import android.content.Intent
import android.net.Uri

/** The most videos a watch_videos link can carry; YouTube builds its temporary playlist from these. */
private const val PLAY_ALL_LIMIT = 50

fun watchUri(vid: String): String = "https://www.youtube.com/watch?v=$vid"

/** A link that YouTube turns into a temporary playlist of the first 50 videos. */
fun playAllUri(vids: List<String>): String =
    "https://www.youtube.com/watch_videos?video_ids=" + vids.take(PLAY_ALL_LIMIT).joinToString(",")

/**
 * Opens [uri] in the YouTube TV app. The two extras make YouTube use its standalone
 * player in a separate task, so one Back closes the player and returns to this app.
 * Without them YouTube reuses its main screen, and Back lands on YouTube's home page.
 */
fun youTubeIntent(uri: String): Intent =
    Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        putExtra("finish_on_ended", true)
        putExtra("force_fullscreen", true)
    }
