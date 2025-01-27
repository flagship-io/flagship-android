package com.abtasty.flagship

import android.content.Context
import com.abtasty.flagship.utils.FlagshipLogManager
import com.abtasty.flagship.utils.HttpCompat
import com.abtasty.flagship.utils.ResponseCompat
import okhttp3.*
import okio.Buffer
import okio.BufferedSource
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

class FlagshipTestsHelper {

    class HttpInterceptor : Interceptor {

        val TAG = "[FlagshipTests Interceptor]"
        val ERROR = "[ERROR]"

        var rules: ConcurrentHashMap<String, Response.Builder> = ConcurrentHashMap()
        var calls: ConcurrentHashMap<String, ArrayList<Pair<Request, ResponseCompat>>> = ConcurrentHashMap()

        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val url: String = HttpCompat.requestUrl(request)

            val response = consume(url, request)?.also { response ->
                println("#INTERCEPTED = " + url)
                call(url, Pair(request, ResponseCompat(response)))
            }
                ?: chain.proceed(request)
                    .also { println("#NOT INTERCEPTED = " + url) }
//            call(url, Pair(request, ResponseCompat(response)))
            return response
        }

        fun intercept(url: String, response: Response.Builder): HttpInterceptor {
            rules[url] = response
            return this
        }

        private fun consume(url: String, request: Request): Response? {
            return rules[url]
                ?.request(request)
                ?.build()
        }

        private fun call(url: String, result: Pair<Request, ResponseCompat>) {
            if (!calls.containsKey(url)) {
                calls[url] = arrayListOf(result)
            } else {
                calls[url]?.add(result)
            }
        }
        public fun calls(url: String): ArrayList<Pair<Request, ResponseCompat>>? {
            return calls[url]
        }
        fun getJsonFromRequestCall(url: String, index: Int): JSONObject? {
            return try {
                HttpCompat.requestJson(calls[url]!![index].first)
            } catch (e: Exception) {
                null
            }
        }

        fun clear() {
            rules.clear()
            calls.clear()
        }

        fun verifyRequestContentOrThrow(url: String, consume: Boolean = true, lambda: (content: JSONObject) -> Unit) {
            if (consume) calls[url]?.removeFirstOrNull() else calls[url]?.firstOrNull()
                ?.also { (request, response) ->
                    try {
                        lambda.invoke(HttpCompat.requestJson(request))
                    } catch (e: Exception) {
                        throw Exception(
                            "$TAG$ERROR Request verification failed:/n%s.".format(
                                FlagshipLogManager.exceptionToString(e)
                            )
                        )
                    }
                } ?: throw Exception("$TAG$ERROR URL(%s) haven't been called.".format(url))
        }
    }

    companion object {

        val emptyResponse = response("", 200)

        var interceptor = HttpInterceptor()

        fun interceptor(): HttpInterceptor {
            return interceptor
        }

        fun response(content: String, code: Int, headers: HashMap<String, String> = HashMap()) : Response.Builder {
            val response = Response.Builder()
            response.code(code)
            response.message("")
            response.protocol(Protocol.HTTP_2)
            for ((k,v) in headers) {
                response.addHeader(k, v)
            }
            response.body(object: ResponseBody() {
                override fun contentLength(): Long {
                    return -1L
                }

                override fun contentType(): MediaType? {
                   return HttpCompat.applicationJsonMediaType()
                }

                override fun source(): BufferedSource {
                    return Buffer().writeUtf8(content)
                }

            })
            return response
        }


        fun responseFromAssets(context : Context, fileName : String, code : Int, headers: HashMap<String, String> = HashMap()): Response.Builder {
            return try {
                val input = context.assets.open(fileName)
                val content =  input.bufferedReader().use { it.readText() }
                response(content, code, headers)
            } catch (e : Exception) {
                e.printStackTrace()
                response("{}", 200)
            }
        }

        fun responseFromString(context : Context, content : String, code : Int, headers: HashMap<String, String> = HashMap()): Response.Builder {
            return try {
                response(content, code, headers)
            } catch (e : Exception) {
                e.printStackTrace()
                response("{}", 200)
            }
        }

        fun jsonArrayFromAssets(context : Context, fileName : String) : JSONArray {
            return try {
                val input = context.assets.open(fileName)
                val content = input.bufferedReader().use { it.readText() }
                JSONArray(content)
            } catch (e : Exception) {
                JSONArray()
            }
        }

        fun jsonObjectFromAssets(context : Context, fileName : String) : JSONObject {
            return try {
                val input = context.assets.open(fileName)
                val content = input.bufferedReader().use { it.readText() }
                JSONObject(content)
            } catch (e : Exception) {
                JSONObject()
            }
        }
    }
}