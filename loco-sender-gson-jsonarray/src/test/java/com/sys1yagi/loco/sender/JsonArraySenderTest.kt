package com.sys1yagi.loco.sender

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.sys1yagi.loco.core.SendingResult
import com.sys1yagi.loco.core.internal.SmashedLog
import io.mockk.coEvery
import io.mockk.slot
import io.mockk.spyk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

internal class JsonArraySenderTest {

    class TestSender(gson: Gson) : JsonArraySender(gson) {
        override suspend fun send(logs: JsonArray): SendingResult {
            return SendingResult.SUCCESS
        }
    }

    @Test
    fun send() = runBlocking {
        val jsonArray = slot<JsonArray>()
        val sender: TestSender = spyk(TestSender(Gson())) {
            coEvery { send(capture(jsonArray)) }.returns(SendingResult.SUCCESS)
        }
        sender.send(
            listOf(
                SmashedLog("", "", "", """{"id":1,"name":"jack"}"""),
                SmashedLog("", "", "", """{"id":2,"name":"jill"}"""),
                SmashedLog("", "", "", """{"id":3,"name":"desugar"}""")
            )
        )
        assertThat(jsonArray.captured.toString()).isEqualTo(
            """
            ["{\"id\":1,\"name\":\"jack\"}","{\"id\":2,\"name\":\"jill\"}","{\"id\":3,\"name\":\"desugar\"}"]
            """.trimIndent()
        )
    }
}
