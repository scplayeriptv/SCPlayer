package com.sc.player.data

import com.sc.player.data.update.BsPatch
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import java.io.ByteArrayOutputStream

class BsPatchTest {
    private fun bz(data: ByteArray): ByteArray {
        val o = ByteArrayOutputStream()
        BZip2CompressorOutputStream(o).use { it.write(data) }
        return o.toByteArray()
    }

    private fun off(v: Long): ByteArray {
        val b = ByteArray(8)
        var x = Math.abs(v)
        for (i in 0 until 8) { b[i] = (x and 0xFF).toByte(); x = x shr 8 }
        if (v < 0) b[7] = (b[7].toInt() or 0x80).toByte()
        return b
    }

    @Test
    fun appliesDiffAndExtra() {
        val old = "HELLO WORLD".toByteArray()
        val new = "HELLO KOTLIN!!".toByteArray()
        // 6 بايت متطابقة (diff=0) ثم 8 بايت extra
        val ctrl = bz(off(6) + off(8) + off(0))
        val diff = bz(ByteArray(6))
        val extra = bz("KOTLIN!!".toByteArray())
        val patch = "BSDIFF40".toByteArray() + off(ctrl.size.toLong()) + off(diff.size.toLong()) +
            off(new.size.toLong()) + ctrl + diff + extra
        assertArrayEquals(new, BsPatch.apply(old, patch))
    }
}
