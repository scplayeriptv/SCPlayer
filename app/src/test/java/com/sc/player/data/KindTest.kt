package com.sc.player.data

import org.junit.Assert.assertEquals
import org.junit.Test

class KindTest {
    private fun ch(group: String, url: String = "http://h/stream/1.mp4") =
        Channel("x", url, group, null, null)

    @Test
    fun xtreamPathsWin() {
        assertEquals(Kind.Movies, ch("Any", "http://h/movie/u/p/1.mp4").kind())
        assertEquals(Kind.Series, ch("Any", "http://h/series/u/p/1.mkv").kind())
    }

    @Test
    fun groupNamesAreUsedOtherwise() {
        assertEquals(Kind.Radio, ch("Arabic Radio").kind())
        assertEquals(Kind.Movies, ch("Action Movies").kind())
        assertEquals(Kind.Series, ch("مسلسلات").kind())
        assertEquals(Kind.Live, ch("Sports").kind())
    }

    @Test
    fun liveUrlsStayLiveEvenInCinemaGroups() {
        assertEquals(Kind.Live, ch("GO BOX CINEMA", "http://h/live/u/p/1.ts").kind())
        assertEquals(Kind.Live, ch("Movies HD", "http://h/u/p/12345").kind())
        assertEquals(Kind.Live, ch("Movies HD", "http://h/u/p/12345.m3u8").kind())
        assertEquals(Kind.Live, ch("أفلام", "http://h:8080/live/u/p/9.m3u8?x=1").kind())
        assertEquals(Kind.Radio, ch("Radio", "http://h/live/u/p/1.ts").kind())
    }
}
