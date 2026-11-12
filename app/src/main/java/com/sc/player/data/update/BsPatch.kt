package com.sc.player.data.update

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream

/** تطبيق باتش بصيغة BSDIFF40 (اللي بتطلّعها bsdiff / bsdiff4) على ملف قديم عشان نطلّع الجديد. */
object BsPatch {
    fun apply(old: ByteArray, patch: ByteArray): ByteArray {
        if (patch.size < 32 || String(patch, 0, 8, Charsets.US_ASCII) != "BSDIFF40") throw IOException("bad patch")
        val ctrlLen = offtin(patch, 8)
        val diffLen = offtin(patch, 16)
        val newSize = offtin(patch, 24)
        if (ctrlLen < 0 || diffLen < 0 || newSize < 0 || newSize > Int.MAX_VALUE) throw IOException("bad header")
        val ctrlStart = 32
        val diffStart = ctrlStart + ctrlLen.toInt()
        val extraStart = diffStart + diffLen.toInt()
        if (extraStart > patch.size) throw IOException("bad header")

        val ctrl = DataInputStream(bz(patch, ctrlStart, diffStart))
        val diff = bz(patch, diffStart, extraStart)
        val extra = bz(patch, extraStart, patch.size)

        val out = ByteArray(newSize.toInt())
        var oldPos = 0L
        var newPos = 0
        val buf = ByteArray(24)
        while (newPos < out.size) {
            ctrl.readFully(buf)
            val add = offtin(buf, 0)
            val copy = offtin(buf, 8)
            val seek = offtin(buf, 16)
            if (add < 0 || copy < 0 || newPos + add > out.size) throw IOException("corrupt patch")
            readFully(diff, out, newPos, add.toInt())
            for (i in 0 until add.toInt()) {
                val o = oldPos + i
                if (o >= 0 && o < old.size) out[newPos + i] = (out[newPos + i] + old[o.toInt()]).toByte()
            }
            newPos += add.toInt()
            oldPos += add
            if (newPos + copy > out.size) throw IOException("corrupt patch")
            readFully(extra, out, newPos, copy.toInt())
            newPos += copy.toInt()
            oldPos += seek
        }
        return out
    }

    private fun bz(data: ByteArray, from: Int, to: Int): InputStream =
        BZip2CompressorInputStream(ByteArrayInputStream(data, from, to - from))

    private fun readFully(input: InputStream, dst: ByteArray, off: Int, len: Int) {
        var n = 0
        while (n < len) {
            val r = input.read(dst, off + n, len - n)
            if (r < 0) throw IOException("short patch")
            n += r
        }
    }

    /** رقم 8 بايت little-endian بإشارة (sign-magnitude) زي bsdiff. */
    internal fun offtin(b: ByteArray, off: Int): Long {
        var y = (b[off + 7].toLong() and 0x7F)
        for (i in 6 downTo 0) y = y * 256 + (b[off + i].toLong() and 0xFF)
        return if (b[off + 7].toInt() and 0x80 != 0) -y else y
    }
}
