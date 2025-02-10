package com.abtasty.flagship

import com.abtasty.flagship.model.CampaignMetadata
import com.abtasty.flagship.model.Variation
import com.abtasty.flagship.model.VariationGroupMetadata
import com.abtasty.flagship.model.VariationMetadata
import junit.framework.TestCase.assertTrue
import org.json.JSONObject
import org.junit.Test

class FlagshipTestsMock : AFlagshipTest() {
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
}