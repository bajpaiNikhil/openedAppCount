package com.example.openedappcount

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppCheckStatTest {

    private val MIN_OPENS = 4
    private val SHORT_SESSION_MS = 30_000L

    @Test
    fun `app opened often for short bursts is flagged reflex`() {
        val opens = listOf(AppOpenCount("com.a", "App A", count = 10))
        val usage = listOf(AppUsageInfo("com.a", "App A", totalTimeInMillis = 100_000L)) // avg = 10_000ms

        val result = computeCheckPatterns(opens, usage, MIN_OPENS, SHORT_SESSION_MS)

        assertEquals(1, result.size)
        assertTrue(result[0].isReflex)
        assertEquals(10_000L, result[0].avgSessionMillis)
    }

    @Test
    fun `app opened often for long sessions is not reflex`() {
        val opens = listOf(AppOpenCount("com.b", "App B", count = 5))
        val usage = listOf(AppUsageInfo("com.b", "App B", totalTimeInMillis = 600_000L)) // avg = 120_000ms

        val result = computeCheckPatterns(opens, usage, MIN_OPENS, SHORT_SESSION_MS)

        assertFalse(result[0].isReflex)
    }

    @Test
    fun `app opened few times is not reflex even if sessions are short`() {
        val opens = listOf(AppOpenCount("com.c", "App C", count = 2))
        val usage = listOf(AppUsageInfo("com.c", "App C", totalTimeInMillis = 5_000L)) // avg = 2_500ms

        val result = computeCheckPatterns(opens, usage, MIN_OPENS, SHORT_SESSION_MS)

        assertFalse(result[0].isReflex)
    }

    @Test
    fun `app with opens but no usage record gets zero time and counts as reflex`() {
        val opens = listOf(AppOpenCount("com.d", "App D", count = 6))
        val usage = emptyList<AppUsageInfo>()

        val result = computeCheckPatterns(opens, usage, MIN_OPENS, SHORT_SESSION_MS)

        assertEquals(0L, result[0].totalTimeMillis)
        assertEquals(0L, result[0].avgSessionMillis)
        assertTrue(result[0].isReflex)
    }

    @Test
    fun `result is sorted by opens descending`() {
        val opens = listOf(
            AppOpenCount("com.low", "Low", count = 2),
            AppOpenCount("com.high", "High", count = 9),
            AppOpenCount("com.mid", "Mid", count = 5),
        )
        val usage = emptyList<AppUsageInfo>()

        val result = computeCheckPatterns(opens, usage, MIN_OPENS, SHORT_SESSION_MS)

        assertEquals(listOf("com.high", "com.mid", "com.low"), result.map { it.packageName })
    }
}
