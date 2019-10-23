package com.abtasty.flagship.api

class BucketingManager {

    companion object {

        fun syncBucketModifications(lambda: () -> (Unit) = {}) {
            ApiManager.sendBucketingRequest()
        }
    }
}