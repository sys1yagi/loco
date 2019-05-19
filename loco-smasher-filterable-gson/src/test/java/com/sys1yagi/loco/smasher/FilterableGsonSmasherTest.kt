package com.sys1yagi.loco.smasher

import com.google.common.truth.Truth
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.sys1yagi.loco.core.LocoLog
import com.sys1yagi.loco.smasher.filter.JsonFilter
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test

internal class FilterableGsonSmasherTest {

    @Test
    fun smash() {
        val smasher = FilterableGsonSmasher(Gson())

        val smashed = smasher.smash(ClickLog(1, "jack"))
        Truth.assertThat(smashed).isEqualTo(
            """
            {"id":1,"name":"jack"}
        """.trimIndent()
        )
    }

    @Test
    fun filter() {
        val smasher = FilterableGsonSmasher(Gson())
        smasher.registerFilter(EventTimeFilter(mockk {
            every { now() } returns 10
        }))
        val smashed = smasher.smash(ClickLog(1, "jack"))
        Truth.assertThat(smashed).isEqualTo(
            """
            {"id":1,"name":"jack","event_time":10}
        """.trimIndent()
        )
    }

    @Test
    fun registerFilter() {
    }

    interface TimeProvider {
        fun now(): Long
    }

    class EventTimeFilter(val timeProvider: TimeProvider) : JsonFilter {
        override fun filter(json: JsonObject): JsonObject {
            json.addProperty("event_time", timeProvider.now())
            return json
        }
    }

    data class ClickLog(
        val id: Int,
        val name: String
    ) : LocoLog
}
