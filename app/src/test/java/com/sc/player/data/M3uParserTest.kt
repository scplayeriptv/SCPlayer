package com.sc.player.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class M3uParserTest {
    @Test
    fun parsesAttributesAndName() {
        val text = """
            #EXTM3U
            #EXTINF:-1 tvg-id="a.1" tvg-logo="http://x/l.png" group-title="News, World",Channel One
            http://host/1.ts
            #EXTINF:-1,Channel Two
            #EXTGRP:Sports
            http://host/2.ts
        """.trimIndent()
        val list = M3uParser.parse(text)
        assertEquals(2, list.size)
        assertEquals("Channel One", list[0].name)
        assertEquals("News, World", list[0].group)
        assertEquals("a.1", list[0].tvgId)
        assertEquals("http://x/l.png", list[0].logo)
        assertEquals("Channel Two", list[1].name)
        assertEquals("Sports", list[1].group)
        assertNull(list[1].logo)
    }

    @Test
    fun ignoresGarbageAndEmpty() {
        assertEquals(0, M3uParser.parse("").size)
        assertEquals(0, M3uParser.parse("<html>not a playlist</html>").size)
    }

    @Test
    fun keepsQuotesInsideTitle() {
        val list = M3uParser.parse("#EXTM3U\n#EXTINF:-1 group-title=\"A\",Channel \"HD\"\nhttp://h/1.ts")
        assertEquals(1, list.size)
        assertEquals("Channel \"HD\"", list[0].name)
        assertEquals("A", list[0].group)
    }

    @Test
    fun parsesLargePlaylistFromSequence() {
        val lines = (0 until 20_000).asSequence().flatMap {
            sequenceOf("#EXTINF:-1 group-title=\"G${it % 50}\",Ch $it", "http://h/$it.ts")
        }
        val list = M3uParser.parse(lines)
        assertEquals(20_000, list.size)
        assertEquals("Ch 19999", list.last().name)
    }

    @Test
    fun attributeKeysAreCaseInsensitiveAndExact() {
        val list = M3uParser.parse(
            "#EXTM3U\n#EXTINF:-1 TVG-ID=\"up.1\" xtvg-id=\"wrong\" TVG-LOGO=\"http://l/p.png\" Group-Title=\"Sports\",Name\nhttp://h/1.ts",
        )
        assertEquals(1, list.size)
        assertEquals("up.1", list[0].tvgId)
        assertEquals("http://l/p.png", list[0].logo)
        assertEquals("Sports", list[0].group)
    }

    @Test
    fun kindIsCachedAndStable() {
        val ch = Channel("A", "http://h/movie/u/p/1.mp4", "X", null, null)
        assertEquals(Kind.Movies, ch.kind())
        assertEquals(Kind.Movies, ch.kind())
    }
}
