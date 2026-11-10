package com.sc.player.data.epg

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class XmltvTimeTest {
    @Test
    fun epochStart() {
        assertEquals(0L, XmltvTime.parse("19700101000000 +0000"))
    }

    @Test
    fun knownDate() {
        assertEquals(946_684_800_000L, XmltvTime.parse("20000101000000 +0000"))
    }

    @Test
    fun appliesOffset() {
        // 12:00 في +0200 هو 10:00 UTC
        assertEquals(XmltvTime.parse("20261110100000 +0000"), XmltvTime.parse("20261110120000 +0200"))
        assertEquals(XmltvTime.parse("20261110130000 +0000"), XmltvTime.parse("20261110120000 -0100"))
    }

    @Test
    fun noOffsetMeansUtc() {
        assertEquals(XmltvTime.parse("20261110120000 +0000"), XmltvTime.parse("20261110120000"))
    }

    @Test
    fun rejectsGarbage() {
        assertNull(XmltvTime.parse("abc"))
        assertNull(XmltvTime.parse(""))
    }
}
