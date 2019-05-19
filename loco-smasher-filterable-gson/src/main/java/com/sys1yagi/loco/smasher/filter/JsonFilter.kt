package com.sys1yagi.loco.smasher.filter

import com.google.gson.JsonObject

interface JsonFilter {
    fun filter(json: JsonObject): JsonObject
}
