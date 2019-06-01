package com.sys1yagi.loco.sender

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.sys1yagi.loco.core.Sender
import com.sys1yagi.loco.core.SendingResult
import com.sys1yagi.loco.core.internal.SmashedLog

abstract class JsonArraySender(private val gson: Gson) : Sender {

    abstract suspend fun send(logs: JsonArray): SendingResult

    override suspend fun send(logs: List<SmashedLog>): SendingResult {
        val jsonArray = JsonArray()
        logs.forEach {
            jsonArray.add(gson.toJsonTree(it.smashedLog))
        }
        return send(jsonArray)
    }
}
