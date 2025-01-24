package com.abtasty.flagship

import androidx.test.core.app.ApplicationProvider
import com.abtasty.flagship.decision.ApiManager
import com.abtasty.flagship.decision.BucketingManager
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.main.FlagshipConfig
import com.abtasty.flagship.utils.FlagshipLogManager
import com.abtasty.flagship.utils.LogManager
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class FlagshipTestsConfig: AFlagshipTest() {
    @Test
    fun test_config() {

        //Test default config
        var config = Flagship.getConfig()
        assert(config.decisionMode == Flagship.DecisionMode.DECISION_API)
        assert(config.envId.isEmpty())
        assert(config.apiKey.isEmpty())
        assert(config.logLevel == LogManager.Level.ALL)
        assert(config.logManager is FlagshipLogManager)
        assert(config.pollingTime == 60L)
        assert(config.pollingUnit == TimeUnit.SECONDS)
        assert(config.statusListener == null)
        assert(config.timeout == 2000L)
        assert(Flagship.getStatus() == Flagship.FlagshipStatus.NOT_INITIALIZED)

        //Test a first config
        runBlocking {
            Flagship.start(
                getApplication(), _ENV_ID_, _API_KEY_, FlagshipConfig.DecisionApi()
                    .withTimeout(4000)
            ).await()
        }
        config = Flagship.getConfig()
        assert(config.envId == _ENV_ID_)
        assert(config.apiKey == _API_KEY_)
        assert(config.timeout == 4000L)
        assert(config.decisionMode == Flagship.DecisionMode.DECISION_API)
        assert(Flagship.getStatus() == Flagship.FlagshipStatus.INITIALIZED)
        assert(Flagship.configManager.decisionManager is ApiManager)

        //Test a second config
        runBlocking {
            Flagship.start(getApplication(), "MY_ENV_ID", "MY_API_KEY", FlagshipConfig.Bucketing()
                .withPollingIntervals(10L, TimeUnit.SECONDS)
                .withLogLevel(LogManager.Level.ALL)
                .withLogManager(object : LogManager() {
                    override fun onLog(level: Level, tag: String, message: String) {
                        println("TEST (test_config) => $tag $message")
                    }
                })
                .withFlagshipStatusListener { _ ->

                }
            ).await()
        }
        config = Flagship.getConfig()
        assert(config.envId == "MY_ENV_ID")
        assert(config.apiKey == "MY_API_KEY")
        assert(config.decisionMode == Flagship.DecisionMode.BUCKETING)
//        assert(Flagship.getStatus() == Flagship.FlagshipStatus.INITIALIZING)
        assert(Flagship.getStatus() == Flagship.FlagshipStatus.INITIALIZED)
        assert(config.logLevel == LogManager.Level.ALL)
        assert(config.logManager !is FlagshipLogManager)
        assert(config.pollingTime == 10L)
        assert(config.pollingUnit == TimeUnit.SECONDS)
        assert(config.statusListener != null)
        assert(config.timeout == 2000L)
        assert(Flagship.configManager.decisionManager is BucketingManager)
    }

    @Test
    fun test_start() {

        FlagshipTestsHelper.interceptor()
            .intercept(
                CAMPAIGNS_URL.format(_ENV_ID_),
                FlagshipTestsHelper.responseFromAssets(getApplication(), "api_response_1.json", 200)
            ).intercept(
                ARIANE_URL,
                FlagshipTestsHelper.response("", 200)
            ).intercept(
                ACTIVATION_URL,
                FlagshipTestsHelper.response("", 200)
            ).intercept(
                BUCKETING_URL.format(_ENV_ID_),
                FlagshipTestsHelper.responseFromAssets(getApplication(), "bucketing_response_1.json", 200)
            )

        //Start API
        runBlocking {
            Flagship.start(getApplication(), _ENV_ID_, _API_KEY_, FlagshipConfig.DecisionApi()).await()
        }
        assert(Flagship.getStatus() == Flagship.FlagshipStatus.INITIALIZED)
        assert(Flagship.getVisitor() == null)


        //Start Bucketing
        var whenStatusReady = CountDownLatch(1)
        runBlocking {
            Flagship.start(getApplication(), _ENV_ID_, _API_KEY_, FlagshipConfig.Bucketing()
                .withFlagshipStatusListener { status ->
                    if (status == Flagship.FlagshipStatus.INITIALIZED)
                        whenStatusReady.countDown()
                }).await()
        }
//        assert(Flagship.getStatus() == Flagship.FlagshipStatus.INITIALIZING)
        assert(Flagship.getStatus() == Flagship.FlagshipStatus.INITIALIZED)
        whenStatusReady.await(500, TimeUnit.MILLISECONDS)
        assert(Flagship.getStatus() == Flagship.FlagshipStatus.INITIALIZED) //todo not ready if 403
        assert(Flagship.getVisitor() == null)

        //// Start Bucketing with server error
        try {
            FlagshipTestsHelper.interceptor().intercept(
                BUCKETING_URL.format(_ENV_ID_),
                FlagshipTestsHelper.responseFromAssets(ApplicationProvider.getApplicationContext(), "", 400)
            )
            whenStatusReady = CountDownLatch(1)
            runBlocking {
                Flagship.start(getApplication(), _ENV_ID_, _API_KEY_, FlagshipConfig.Bucketing()
                    .withFlagshipStatusListener { status ->
                        if (status == Flagship.FlagshipStatus.INITIALIZED)
                            whenStatusReady.countDown()
                    })
            }
            whenStatusReady.await(500, TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            assert(false)
        }
    }
}