package com.abtasty.flagship

import okhttp3.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

class HttpInterceptor : Interceptor {

    class Rule(var url: String, private val callback: ((Request, Int) -> (Response?))) {

        var calls = 0

        fun proceed(interceptedRequest: Request): Response? {
            calls++
            return callback.invoke(interceptedRequest, calls)
        }
    }

    private var rules = HashMap<String, Rule>()

    fun addRule(rule: Rule) {
        rules[rule.url] = rule
    }

    fun removeRule(rule: Rule) {
        rules.remove(rule.url)
    }

    fun removeRule(url: String) {
        rules.remove(url)
    }

    fun removeAllRules() {
        rules.clear()
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url: String = apiCompatibility.requestUrl(request).toString()
        return if (rules.containsKey(url))
            rules[url]?.proceed(request) ?: chain.proceed(request)
        else
            chain.proceed(request)
    }
}