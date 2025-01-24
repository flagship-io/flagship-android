package com.abtasty.flagship.model

import com.abtasty.flagship.visitor.VisitorDelegate
import org.json.JSONArray
import org.json.JSONObject

/**
 * This class is a collection of Flags.
 */
class FlagCollection(private val visitor: VisitorDelegate, private val flagMap: HashMap<String, Flag> = HashMap()) {

    /**
     * Return all the Flag keys from the collection.
     */
    fun keys(): MutableSet<String> {
        return flagMap.keys
    }

    /**
     * Return the Flag from the collection corresponding to the given key
     *
     * @param key Flag key.
     */
    operator fun get(key: String): Flag {
        return flagMap[key] ?: Flag(visitor, key)
    }

    /**
     * Expose all the flags in the collection. Only the ones whose value has been consumed will be exposed.
     */
    fun exposeAll() {
        flagMap.values.forEach { it.visitorExposed() }
    }

    /**
     * Return a new FlagCollection base on the given filter.
     */
    fun filter(predicate: (Map.Entry<String, Flag>) -> Boolean): FlagCollection {
        return FlagCollection(visitor, HashMap(flagMap.filter(predicate)))
    }

    /**
     * Return the list of Flag FlagMetadata inside the collection.
     */
    fun metadata(): Map<String, FlagMetadata> {
        return flagMap.mapValues { (k, v) -> v.metadata() }
    }

    /**
     * Return the collection Flags metadata as JSONObject.
     */
    @OptIn(ExperimentalStdlibApi::class)
    fun toJSON(): JSONObject {
        val result = JSONObject()
        flagMap.values.forEach {
            val hex = JSONObject()
                .put("v", visitor.flags[it.key]?.value ?: JSONObject.NULL)
            result.put(
                it.key, it.metadata().toJson()
                    .put("key", it.key)
                    .put("hex", hex.toString().toByteArray().toHexString(HexFormat.Default))
            )
        }
        return result
    }

    /**
     * Return a FlagCollection iterator.
     */
    operator fun iterator(): Iterator<Pair<String, Flag>> {
        return object: Iterator<Pair<String, Flag>> {

            private val keys = flagMap.keys.toList()
            private var index = 0

            override fun hasNext(): Boolean {
                return index < keys.size
            }

            override fun next(): Pair<String, Flag> {
                val key = keys[index]
                val flag = flagMap[key]!!
                index++
                return Pair(key, flag)
            }

        }
    }

    /**
     * Return the collection size.
     */
    fun size(): Int {
        return flagMap.size
    }

}