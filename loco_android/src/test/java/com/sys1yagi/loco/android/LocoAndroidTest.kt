package com.sys1yagi.loco.android

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.sys1yagi.loco.core.*
import com.sys1yagi.loco.core.internal.SmashedLog
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows


class LocoAndroidTest {

    @AfterEach
    fun tearDown() {
        LocoAndroid.stop()
    }

    @Test
    fun initialize() {
        // it is sample code
        LocoAndroid.start(
            LocoConfig(
                store = TestStore(),
                smashers = listOf(
                    JsonSmasher(Gson())
                ),
                senders = listOf(
                    LogcatSender()
                )
            ) {
                logToSmasher[JsonSmasher::class] = listOf(
                    ClickLog::class
                )
                logToSender[LogcatSender::class] = listOf(
                    ClickLog::class
                )
            }
        )
        LocoAndroid.send(
            ClickLog(1, "jack")
        )
    }

    @Test
    fun notYetInitialized() {
        val error = assertThrows<IllegalStateException>("error") {
            LocoAndroid.send(
                ClickLog(1, "jack")
            )
        }
        assertThat(error.message).isEqualTo(
            """
            Loco is not yet initialized.
            Please call start(config: LocoConfig) first.
        """.trimIndent()
        )
    }

    // TODO alreadyInitialized

    @Test
    fun smasherMapping() {
        val smasher: Smasher = mockk(relaxed = true)
        LocoAndroid.start(
            LocoConfig(
                store = TestStore(),
                smashers = listOf(
                    smasher
                ),
                senders = listOf(
                    LogcatSender()
                )
            ) {
                logToSmasher[smasher::class] = listOf(ClickLog::class)
                logToSender[LogcatSender::class] = listOf(
                    ClickLog::class
                )
            }
        )
        LocoAndroid.send(
            ClickLog(1, "jack")
        )
        verify { smasher.smash(any()) }
    }

    @Test
    fun senderMappingMissing() {
        val smasher: Smasher = mockk(relaxed = true)
        val sender: Sender = mockk(relaxed = true)
        LocoAndroid.start(
            LocoConfig(
                store = TestStore(),
                smashers = listOf(
                    smasher
                ),
                senders = listOf(
                    sender
                )
            ) {
                logToSmasher[smasher::class] = listOf(ClickLog::class)
            }
        )
        val error = assertThrows<IllegalStateException>("error") {
            LocoAndroid.send(
                ClickLog(1, "jack")
            )
        }
        assertThat(error.message).isEqualTo(
            """
            Missing mapping to Sender Type.
            Set up the mapping of com.sys1yagi.loco.android.LocoAndroidTest${"$"}ClickLog class.
        """.trimIndent()
        )
    }

    @Test
    fun store() = runBlockingTest {
        val smasher: Smasher = mockk(relaxed = true)
        val sender: Sender = mockk(relaxed = true)
        val store: Store = mockk(relaxed = true)
        LocoAndroid.start(
            LocoConfig(
                store = store,
                smashers = listOf(
                    smasher
                ),
                senders = listOf(
                    sender
                )
            ) {
                logToSmasher[smasher::class] = listOf(ClickLog::class)
                logToSender[sender::class] = listOf(ClickLog::class)
            },
            this
        )
        LocoAndroid.send(
            ClickLog(1, "jack")
        )
        coVerify { store.store(any()) }
        LocoAndroid.stop()
    }

    // TODO filter

    // TODO sending

    // TODO checker

    // TODO buffering

    data class ClickLog(
        val id: Int,
        val name: String
    ) : LocoLog

    class JsonSmasher(val gson: Gson) : Smasher {
        private val filters = mutableListOf<Filter>()
        override fun smash(log: LocoLog): String {
            return gson.toJson(log)
        }

        override fun registerFilter(filter: Filter) {
            filters.add(filter)
        }
    }

    class LogcatSender : Sender {
        override fun send() {
            // no op
        }
    }

    class TestStore : Store {
        override suspend fun store(log: SmashedLog) {
            // no op
        }

        override suspend fun load(size: Int): List<SmashedLog> {
            return emptyList()
        }

        override suspend fun delete(logs: List<SmashedLog>) {
            // no op
        }
    }
}
