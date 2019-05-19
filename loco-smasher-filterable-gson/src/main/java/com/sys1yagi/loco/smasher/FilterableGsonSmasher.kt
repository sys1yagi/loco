package com.sys1yagi.loco.smasher

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.sys1yagi.loco.core.LocoLog
import com.sys1yagi.loco.core.Smasher
import com.sys1yagi.loco.smasher.filter.JsonFilter

class FilterableGsonSmasher(private val gson: Gson) : Smasher {
    private val filters = mutableListOf<JsonFilter>()
    override fun smash(log: LocoLog): String {
        val jsonObject = gson.toJsonTree(log) as JsonObject
        filters.forEach {
            it.filter(jsonObject)
        }
        return jsonObject.toString()
    }

    fun registerFilter(filter: JsonFilter) {
        filters.add(filter)
    }
}
