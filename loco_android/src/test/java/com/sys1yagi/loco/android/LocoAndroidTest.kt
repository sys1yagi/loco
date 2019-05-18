package com.sys1yagi.loco.android

import android.util.Log
import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.sys1yagi.loco.core.*
import com.sys1yagi.loco.core.internal.SmashedLog
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.AfterEach
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
        val store: Store = mockk(relaxed = true) {
            coEvery { load(any()) }.returns(emptyList())
        }
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
        LocoAndroid.send(
            ClickLog(2, "jill")
        )
        LocoAndroid.send(
            ClickLog(3, "desuger")
        )
        LocoAndroid.stop()
        coVerify(exactly = 3) { store.store(any()) }
    }

    @Test
    fun send() = runBlockingTest {
        val smasher: Smasher = mockk(relaxed = true)
        val sender: Sender = mockk(relaxed = true) {
            coEvery { send(any()) } returns SendingResult.SUCCESS
        }
        val store = TestStore()
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
        LocoAndroid.send(
            ClickLog(2, "jill")
        )
        LocoAndroid.send(
            ClickLog(3, "desuger")
        )

        assertThat(store.storage.size).isEqualTo(3)

        advanceTimeBy(1500)

        LocoAndroid.stop()
        coVerify { sender.send(any()) }
        assertThat(store.storage.size).isEqualTo(0)
    }

    // TODO failed

    // TODO retry

    // TODO filter

    // TODO checker

    // TODO multi sender

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
        override suspend fun send(logs: List<SmashedLog>): SendingResult {
            logs.forEach {
                Log.d("LogcatSender", it.toString())
            }
            return SendingResult.SUCCESS
        }
    }

    class TestStore : Store {
        val storage = mutableListOf<SmashedLog>()
        override suspend fun store(log: SmashedLog) {
            storage.add(log)
        }

        override suspend fun load(size: Int): List<SmashedLog> {
            return storage.take(size)
        }

        override suspend fun delete(logs: List<SmashedLog>) {
            storage.removeAll(logs)
        }
    }
}
