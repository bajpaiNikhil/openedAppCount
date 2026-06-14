package com.example.openedappcount

import org.junit.Assert.assertEquals
import org.junit.Test

class AppUsageRowTest {

    @Test
    fun `sub-minute durations render as seconds`() {
        assertEquals("38s", shortAvg(38_000L))
        assertEquals("0s", shortAvg(0L))
    }

    @Test
    fun `durations with leftover seconds render minutes and seconds`() {
        assertEquals("1m 50s", shortAvg(110_000L))
    }

    @Test
    fun `exact-minute durations render without seconds`() {
        assertEquals("4m", shortAvg(240_000L))
    }
}
