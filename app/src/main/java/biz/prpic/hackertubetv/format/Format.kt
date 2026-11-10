package biz.prpic.hackertubetv.format

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeParseException

// These functions reproduce the formatting of hackertube.org so a card on the TV reads
// the same as on the site. Missing values give an empty string, never "null". Numbers
// are built without String.format so the TV's locale cannot change the digits or the
// decimal separator.

/** "5:25", "48:00" or "1:46:39"; empty for a missing or zero duration. */
fun formatDuration(seconds: Int?): String {
    if (seconds == null || seconds <= 0) return ""
    val hours = seconds / 3600
    val minutes = seconds % 3600 / 60
    val secs = seconds % 60
    return if (hours > 0) {
        "$hours:${twoDigits(minutes)}:${twoDigits(secs)}"
    } else {
        "$minutes:${twoDigits(secs)}"
    }
}

/** "999 views", "41K views", "4.2M views", "1.5B views"; empty when unknown. */
fun formatViews(views: Long?): String {
    if (views == null) return ""
    val short = when {
        views >= 1_000_000_000 -> oneDecimal(views / 1e9) + "B"
        views >= 1_000_000 -> oneDecimal(views / 1e6) + "M"
        views >= 1_000 -> Math.round(views / 1e3).toString() + "K"
        else -> views.toString()
    }
    return "$short views"
}

/** "3 days ago", "1 year ago" or "today"; empty for a missing or unreadable date. */
fun formatAgo(publishedIso: String?, nowMillis: Long): String {
    val publishedMillis = parseIsoMillis(publishedIso) ?: return ""
    val seconds = (nowMillis - publishedMillis) / 1000
    for ((unit, unitSeconds) in AGE_UNITS) {
        val n = seconds / unitSeconds
        if (n >= 1) return "$n $unit${if (n > 1) "s" else ""} ago"
    }
    return "today"
}

/** A whole number with a comma between thousands, whatever the TV's locale. */
fun formatCount(count: Int): String {
    val digits = kotlin.math.abs(count.toLong()).toString()
    val grouped = digits.reversed().chunked(3).joinToString(",").reversed()
    return if (count < 0) "-$grouped" else grouped
}

/** Total length of a playlist: "45 min", "2 hr" or "2 hr 5 min". */
fun formatHours(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours == 0 -> "$rest min"
        rest == 0 -> "$hours hr"
        else -> "$hours hr $rest min"
    }
}

/** The grey line under a card title, for example "11M views • 1 year ago". */
fun videoSubline(views: Long?, publishedIso: String?, nowMillis: Long): String =
    listOf(formatViews(views), formatAgo(publishedIso, nowMillis))
        .filter { it.isNotEmpty() }
        .joinToString(" • ")

// Fixed-length units, the same approximation the site uses (a month is 30 days).
private val AGE_UNITS = listOf(
    "year" to 31_536_000L,
    "month" to 2_592_000L,
    "week" to 604_800L,
    "day" to 86_400L,
)

private fun twoDigits(value: Int): String = value.toString().padStart(2, '0')

// Rounds half up on the exact value and drops a trailing ".0", like the site's
// toFixed(1) followed by removing ".0".
private fun oneDecimal(value: Double): String =
    BigDecimal(value).setScale(1, RoundingMode.HALF_UP).toPlainString().removeSuffix(".0")

// Accepts a full ISO timestamp with "Z" or an offset, or a plain date (read as UTC
// midnight), which covers what a browser's Date accepts for this field.
private fun parseIsoMillis(text: String?): Long? {
    if (text.isNullOrBlank()) return null
    return try {
        OffsetDateTime.parse(text).toInstant().toEpochMilli()
    } catch (_: DateTimeParseException) {
        try {
            LocalDate.parse(text).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        } catch (_: DateTimeParseException) {
            null
        }
    }
}
