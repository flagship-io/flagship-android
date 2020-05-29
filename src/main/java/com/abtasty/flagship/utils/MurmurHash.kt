package com.abtasty.flagship.utils

class MurmurHash {

    companion object {

        fun murmurhash3_x86_32(data: String): Long {

            val c1 = -0x3361d2af
            val c2 = 0x1b873593

            var h1 = 0

            var pos = 0
            val end = data.length
            var k1 = 0
            var k2 = 0
            var shift = 0
            var bits = 0
            var nBytes = 0

            while (pos < end) {
                val code = data[pos++].toInt()

                if (code < 0x80) {
                    k2 = code
                    bits = 8
                } else if (code < 0x800) {
                    k2 = (0xC0 or (code shr 6)
                            or (0x80 or (code and 0x3F) shl 8))
                    bits = 16
                } else if (code < 0xD800 || code > 0xDFFF || pos >= end) {
                    k2 = (0xE0 or (code shr 12)
                            or (0x80 or (code shr 6 and 0x3F) shl 8)
                            or (0x80 or (code and 0x3F) shl 16))
                    bits = 24
                } else {
                    var utf32 = data[pos++].toInt()
                    utf32 = (code - 0xD7C0 shl 10) + (utf32 and 0x3FF)
                    k2 = (0xff and (0xF0 or (utf32 shr 18))
                            or (0x80 or (utf32 shr 12 and 0x3F) shl 8
                            ) or (0x80 or (utf32 shr 6 and 0x3F) shl 16
                            ) or (0x80 or (utf32 and 0x3F) shl 24))
                    bits = 32
                }

                k1 = k1 or (k2 shl shift)

                shift += bits

                if (shift >= 32) {
                    k1 *= c1
                    k1 = (k1 shl 15) or (k1 ushr 17)
                    k1 *= c2
                    h1 = h1 xor k1
                    h1 = (h1 shl 13) or (h1 ushr 19)
                    h1 = (h1 * 5) + (-0x19ab949c)
                    shift -= 32

                    k1 = if (shift != 0) {
                        k2 ushr (bits - shift)
                    } else 0
                    nBytes += 4
                }
            }

            if (shift > 0) {
                nBytes += (shift shr 3)
                k1 *= c1
                k1 = (k1 shl 15) or (k1 ushr 17)
                k1 *= c2
                h1 = h1 xor k1
            }

            h1 = h1 xor nBytes

            h1 = h1 xor (h1 ushr 16)
            h1 *= -0x7a143595
            h1 = h1 xor (h1 ushr 13)
            h1 *= -0x3d4d51cb
            h1 = h1 xor (h1 ushr 16)

            return (h1.toLong() and 0xFFFFFFFFL)
        }
    }
}