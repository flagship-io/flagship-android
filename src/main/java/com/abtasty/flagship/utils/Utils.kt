package com.abtasty.flagship.utils

import android.content.Context
import androidx.core.os.ConfigurationCompat
import com.abtasty.flagship.api.Hit
import com.abtasty.flagship.main.Flagship
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.HashMap

class Utils {

    companion object {

        internal fun loadDeviceContext(context: Context) {
            loadDeviceResolution(context)
            loadLocale(context)
        }

        private fun loadDeviceResolution(context: Context) {
            val displayMetrics = context.resources.displayMetrics
            Flagship.deviceContext[Hit.KeyMap.DEVICE_RESOLUTION.key] = "${displayMetrics.widthPixels}x${displayMetrics.heightPixels}"
        }

        private fun loadLocale(context: Context) {
            val locale = ConfigurationCompat.getLocales(context.resources.configuration)[0]
            Flagship.deviceContext[Hit.KeyMap.DEVICE_LOCALE.key] = locale.toString().toLowerCase().replace("_", "-")
        }

        internal fun logFailorSuccess(boolean: Boolean) : String {
            return if (boolean) "Success" else "Fail"
        }

        fun genVisitorId(context: Context): String? {

            val sharedPref = context.getSharedPreferences("_Flagship", Context.MODE_PRIVATE)
            var visitorId = sharedPref.getString("visitorId", "")
            if (visitorId.isEmpty()) {

                val cal = Calendar.getInstance()
                val min = 10000
                val max = 99999
                val random = Random().nextInt(max - min + 1) + min
                visitorId = "${cal.get(Calendar.YEAR)}" +
                        "${cal.get(Calendar.MONTH)}" +
                        "${cal.get(Calendar.DAY_OF_MONTH)}" +
                        "${cal.get(Calendar.HOUR_OF_DAY)}" +
                        "${cal.get(Calendar.MINUTE)}" +
                        "${cal.get(Calendar.SECOND)}" +
                        "$random"
                val edit = sharedPref.edit()
                edit.putString("visitorId", visitorId)
                edit.apply()
            }
            return visitorId
        }

        fun murmurHash3() {
            GlobalScope.launch {
                var list = ArrayList<String>()
                for (i in 1000000..1000300) {
                    var str = "zekze$i"
                    var hash = Murmur.murmurhash3_x86_32(str, 0, str.length, 1)
                    var hashstr = hash.toString()
                    list.add(hashstr.substring(hashstr.length-2 , hashstr.length))
//                    list.add(hashstr)

                }
                list.sort()
                for (i in 0 until list.size) {
                    println("#M= " + list[i])
                }
            }
        }

        fun murmurhash3_x86_32(data: CharSequence): Long {

            val c1 = -0x3361d2af
            val c2 = 0x1b873593

//            var h1 = seed
//
//            var pos = offset
//            val end = offset + len
            var h1 = 1
            var pos = 0
            val end = pos + data.length

            var k1 = 0
            var k2 = 0
            var shift = 0
            var bits = 0
            var nBytes = 0   // length in UTF8 bytes


            while (pos < end) {
                val code = data[pos++].toInt()
                if (code < 0x80) {
                    k2 = code
                    bits = 8

                } else if (code < 0x800) {
                    k2 = 0xC0 or (code shr 6) or (0x80 or (code and 0x3F) shl 8)
                    bits = 16
                } else if (code < 0xD800 || code > 0xDFFF || pos >= end) {
                    // we check for pos>=end to encode an unpaired surrogate as 3 bytes.
                    k2 = (0xE0 or (code shr 12)
                            or (0x80 or (code shr 6 and 0x3F) shl 8)
                            or (0x80 or (code and 0x3F) shl 16))
                    bits = 24
                } else {
                    // surrogate pair
                    // int utf32 = pos < end ? (int) data.charAt(pos++) : 0;
                    var utf32 = data[pos++].toInt()
                    utf32 = (code - 0xD7C0 shl 10) + (utf32 and 0x3FF)
                    k2 = (0xff and (0xF0 or (utf32 shr 18))
                            or (0x80 or (utf32 shr 12 and 0x3F) shl 8)
                            or (0x80 or (utf32 shr 6 and 0x3F) shl 16)
                            or (0x80 or (utf32 and 0x3F) shl 24))
                    bits = 32
                }


                k1 = k1 or (k2 shl shift)

                // int used_bits = 32 - shift;  // how many bits of k2 were used in k1.
                // int unused_bits = bits - used_bits; //  (bits-(32-shift)) == bits+shift-32  == bits-newshift

                shift += bits
                if (shift >= 32) {
                    // mix after we have a complete word

                    k1 *= c1
                    k1 = k1 shl 15 or k1.ushr(17)  // ROTL32(k1,15);
                    k1 *= c2

                    h1 = h1 xor k1
                    h1 = h1 shl 13 or h1.ushr(19)  // ROTL32(h1,13);
                    h1 = h1 * 5 + -0x19ab949c

                    shift -= 32
                    // unfortunately, java won't let you shift 32 bits off, so we need to check for 0
                    if (shift != 0) {
                        k1 = k2.ushr(bits - shift)   // bits used == bits - newshift
                    } else {
                        k1 = 0
                    }
                    nBytes += 4
                }

            } // inner

            // handle tail
            if (shift > 0) {
                nBytes += shift shr 3
                k1 *= c1
                k1 = k1 shl 15 or k1.ushr(17)  // ROTL32(k1,15);
                k1 *= c2
                h1 = h1 xor k1
            }

            // finalization
            h1 = h1 xor nBytes

            // fmix(h1);
            h1 = h1 xor h1.ushr(16)
            h1 *= -0x7a143595
            h1 = h1 xor h1.ushr(13)
            h1 *= -0x3d4d51cb
            h1 = h1 xor h1.ushr(16)

            return (h1 and (0xFFFFFFFF).toInt()).toLong()
        }
    }
}