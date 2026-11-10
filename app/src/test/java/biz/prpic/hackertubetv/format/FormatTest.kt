package biz.prpic.hackertubetv.format

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {
    @Test fun duration() {
        assertEquals("48:00", formatDuration(2880)); assertEquals("1:46:39", formatDuration(6399))
        assertEquals("5:25", formatDuration(325)); assertEquals("1:00:00", formatDuration(3600))
        assertEquals("", formatDuration(0)); assertEquals("", formatDuration(null))
    }

    @Test fun views() {
        assertEquals("41K views", formatViews(41205)); assertEquals("11M views", formatViews(10_997_452))
        assertEquals("4.2M views", formatViews(4_246_622)); assertEquals("1.5B views", formatViews(1_500_000_000))
        assertEquals("999 views", formatViews(999)); assertEquals("", formatViews(null))
    }

    @Test fun ago() {
        val now = Instant.parse("2026-11-10T12:00:00Z").toEpochMilli()
        assertEquals("8 years ago", formatAgo("2017-12-08T00:00:00Z", now))
        assertEquals("1 year ago", formatAgo("2025-02-10T00:00:00Z", now))
        assertEquals("2 months ago", formatAgo("2026-09-01T00:00:00Z", now))
        assertEquals("1 week ago", formatAgo("2026-11-01T00:00:00Z", now))
        assertEquals("3 days ago", formatAgo("2026-11-07T10:00:00Z", now))
        assertEquals("today", formatAgo("2026-11-10T08:00:00Z", now))
        assertEquals("", formatAgo(null, now)); assertEquals("", formatAgo("not a date", now))
    }

    @Test fun count() {
        assertEquals("0", formatCount(0)); assertEquals("999", formatCount(999))
        assertEquals("1,234", formatCount(1234)); assertEquals("1,234,567", formatCount(1_234_567))
        assertEquals("100,000", formatCount(100_000))
    }

    @Test fun hours() { assertEquals("2 hr 5 min", formatHours(125)); assertEquals("45 min", formatHours(45)) }

    @Test fun subline() {
        val now = Instant.parse("2026-11-10T12:00:00Z").toEpochMilli()
        assertEquals("11M views • 1 year ago", videoSubline(10_997_452, "2025-02-10T15:00:00Z", now))
        assertEquals("1 year ago", videoSubline(null, "2025-02-10T15:00:00Z", now))
        assertEquals("", videoSubline(null, null, now))
    }
}
