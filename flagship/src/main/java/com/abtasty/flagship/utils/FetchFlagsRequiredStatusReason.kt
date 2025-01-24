package com.abtasty.flagship.utils

enum class FetchFlagsRequiredStatusReason {
    VISITOR_CONTEXT_UPDATED,
    VISITOR_AUTHENTICATED,
    VISITOR_UNAUTHENTICATED,
    FLAGS_NEVER_FETCHED,
    FLAGS_FETCHING_ERROR,
    FLAGS_FETCHED_FROM_CACHE,
    NONE
}
