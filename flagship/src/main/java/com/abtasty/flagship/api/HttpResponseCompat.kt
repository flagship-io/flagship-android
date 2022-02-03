package com.abtasty.flagship.api

import okhttp3.Headers
import okhttp3.Response

abstract class HttpResponseCompat(protected var response: Response) {

    var code : Int = 0
    var url : String = ""
    var method : String = ""
    var time : Long = 0L
    var headers : Headers? = null
    var content : String? = null

    var requestContent : String = ""
    var requestHeaders : Headers? = null
}