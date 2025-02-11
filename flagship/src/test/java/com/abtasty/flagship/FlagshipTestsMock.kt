package com.abtasty.flagship

import com.abtasty.flagship.AFlagshipTest.Companion._API_KEY_
import com.abtasty.flagship.AFlagshipTest.Companion._ENV_ID_
import com.abtasty.flagship.main.Flagship
import com.abtasty.flagship.main.FlagshipConfig
import com.abtasty.flagship.model.CampaignMetadata
import com.abtasty.flagship.model.Variation
import com.abtasty.flagship.model.VariationGroupMetadata
import com.abtasty.flagship.model.VariationMetadata
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
@Config(sdk = [24])
class FlagshipTestsMock {
        @Test
        fun variations() {
            val v = Variation.parse(JSONObject("{}"), false, VariationGroupMetadata("", "", CampaignMetadata()))
            val v2 = Variation.parse(JSONObject("{\n" +
                    "        \"id\": \"bu6lgeu3bdt01555555\",\n" +
                    "        \"modifications\": {\n" +
                    "          \"type\": \"JSON\",\n" +
                    "          \"value\": {\n" +
                    "            \"target\": \"is\"\n" +
                    "          }\n" +
                    "        },\n" +
                    "        \"reference\": false\n" +
                    "      }"), false, VariationGroupMetadata("", "", CampaignMetadata()))
            assertTrue(v == null)
            assertTrue(v2.toString().isNotEmpty())
            val flags = Variation.parse_flags(
                JSONObject("{}"),
                VariationMetadata("", "", false, 100, VariationGroupMetadata("", "", CampaignMetadata()))
            )
            assertTrue(flags == null)
        }

        @Test
        fun initHttpClient() {
            runBlocking {
                Flagship.start(RuntimeEnvironment.getApplication(), _ENV_ID_, _API_KEY_, FlagshipConfig.DecisionApi()).await()
            }
            assert(Flagship.getStatus() == Flagship.FlagshipStatus.INITIALIZED)
        }
}