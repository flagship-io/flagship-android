package com.abtasty.flagship

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.abtasty.flagship.api.HttpCompat
import okhttp3.*
import okio.Buffer
import okio.BufferedSource
import org.json.JSONArray
import org.json.JSONObject
import java.lang.Error

class FlagshipTestsHelper {

    class HttpInterceptor : Interceptor {

        //        data class Rule(var url: String, var block: ((Request, Int) -> (Response))? = null, var response: Response.Builder?) {
        data class Rule(var url: String) {

            var responseLambda : ((Request, Int) -> (Response.Builder))? = null
            var requestLambda : ((Request, Int) -> (Unit))? = null
            var response: Response.Builder? = null
            var errors : Throwable? = null

            class Builder(var url: String) {

                var rule = Rule(url)

                fun returnResponse(lambda: ((Request, Int) -> (Response.Builder))): Builder {
                    rule.responseLambda = lambda
                    return this
                }

                fun returnResponse(response: Response.Builder): Builder {
                    rule.response = response
                    return this
                }

                fun verifyRequest(lambda: ((Request, Int) -> (Unit))): Builder {
                    rule.requestLambda = lambda
                    return this
                }

                fun build() : Rule {
                    return rule
                }
            }

            enum class Type(var value : Int) {
                ALL(0), NB_CALL(1)
            }

            fun proceed(interceptedRequest: Request, totalCall : Int): Response? {
//                System.out.println("Intercepted => " + interceptedRequest.url)
                return try {
                    if (requestLambda != null) requestLambda?.invoke(interceptedRequest, totalCall)
                    when (true) {
                        (response != null) -> response?.request(interceptedRequest)?.build()
                        (responseLambda != null) -> responseLambda?.invoke(interceptedRequest, totalCall)?.request(interceptedRequest)?.build()
                        else -> null
                    }
                } catch (e : Error) {
                    errors = e
                    null
                } catch (e : Exception) {
                    errors = e
                    null
                }
            }
        }

        data class Rules(var url : String) {

            private var calls : Int = 0
            private var ruleAll : Rule? = null
            private var rulesByIndex = HashMap<Int, Rule>()

            fun addRule(index: Int, rule: Rule) {
                rulesByIndex[index] = rule
            }

            fun addRule(rule: Rule) {
                ruleAll = rule
            }

            fun proceed(interceptedRequest: Request) : Response? {
                calls += 1
                return if (rulesByIndex.isNotEmpty())
                    rulesByIndex[calls]?.proceed(interceptedRequest, calls)
                else
                    ruleAll?.proceed(interceptedRequest, calls)
            }

            fun checkErrors() {
                ruleAll?.errors?.let { throw it }
                for ((url, rule) in rulesByIndex){
                    rule.errors?.let { throw it }
                }
            }
        }

        private val rules = HashMap<String, Rules>()


        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            val url: String = HttpCompat.requestUrl(request)
            System.out.println("#D url : " + url)
            return rules[url]?.proceed(request) ?: chain.proceed(request)
        }

        private fun addRules(url: String) {
            if (!rules.containsKey(url))
                rules[url] = Rules(url)
        }

        fun addRule(index: Int, rule: Rule): Rule {
            addRules(rule.url)
            rules[rule.url]?.addRule(index, rule)
            return rule
        }

        fun addRule(rule: Rule): Rule {
            addRules(rule.url)
            rules[rule.url]?.addRule(rule)
            return rule
        }

        fun clearRules() {
            rules.clear()
        }

        fun rules(): HashMap<String, Rules> {
            return rules
        }

    }

    companion object {

        val emptyResponse = response("", 200)

        internal var interceptor = HttpInterceptor()

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