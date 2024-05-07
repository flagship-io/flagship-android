package com.abtasty.flagship.visitor

import java.util.HashMap

data class VisitorExposed(
    val visitorId: String,
    val anonymousId: String?,
    val context: HashMap<String, Any>,
    val isAuthenticated: Boolean,
    val hasConsented: Boolean
)