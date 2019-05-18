package com.sys1yagi.loco

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.sys1yagi.loco.core.*
import com.sys1yagi.loco.core.internal.SmashedLog
import io.mockk.*
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows


class LocoTest {

    @AfterEach
    fun tearDown() {
        Loco.stop()
    }

    @Test
    fun initialize() {
        // it is sample code
        Loco.start(
            LocoConfig(
                store = TestStore(),
                smashers = listOf(
                    JsonSmasher(Gson())
                ),
                senders = listOf(
                    StdOutSender()
                )
            ) {
                logToSmasher[JsonSmasher::class] = listOf(
                    ClickLog::class
                )
                logToSender[StdOutSender::class] = listOf(
                    ClickLog::class
                )
            }
        )
        Loco.send(
            ClickLog(1, "jack")
        )
    }

    @Test
    fun notYetInitialized() {
        val error = assertThrows<IllegalStateException>("error") {
            Loco.send(
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

    @Test
    fun alreadyInitialized() {
        Loco.start(
            LocoConfig(
                store = TestStore(),
                smashers = listOf(
                    JsonSmasher(Gson())
                ),
                senders = listOf(
                    StdOutSender()
                )
            ) {
                logToSmasher[JsonSmasher::class] = listOf(
                    ClickLog::class
                )
                logToSender[StdOutSender::class] = listOf(
                    ClickLog::class
                )
            }
        )
        val error = assertThrows<IllegalStateException>("error") {
            Loco.start(
                mockk()
            )
        }
        assertThat(error.message).isEqualTo(
            """
            Loco is already initialized.
        """.trimIndent()
        )
    }

    @Test
    fun smasherMapping() {
        val smasher: Smasher = mockk(relaxed = true)
        Loco.start(
            LocoConfig(
                store = TestStore(),
                smashers = listOf(
                    smasher
                ),
                senders = listOf(
                    StdOutSender()
                )
            ) {
                logToSmasher[smasher::class] = listOf(ClickLog::class)
                logToSender[StdOutSender::class] = listOf(
                    ClickLog::class
                )
            }
        )
        Loco.send(
            ClickLog(1, "jack")
        )
        verify { smasher.smash(any()) }
    }

    @Test
    fun senderMappingMissing() {
        val smasher: Smasher = mockk(relaxed = true)
        val sender: Sender = mockk(relaxed = true)
        Loco.start(
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
            Loco.send(
                ClickLog(1, "jack")
            )
        }
        assertThat(error.message).isEqualTo(
            """
            Missing mapping to Sender Type.
            Set up the mapping of com.sys1yagi.loco.LocoTest${"$"}ClickLog class.
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
        Loco.start(
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
        Loco.send(
            ClickLog(1, "jack")
        )
        Loco.send(
            ClickLog(2, "jill")
        )
        Loco.send(
            ClickLog(3, "desuger")
        )
        Loco.stop()
        coVerify(exactly = 3) { store.store(any()) }
    }

    @Test
    fun sendSuccess() = runBlockingTest {
        val smasher: Smasher = mockk(relaxed = true)
        val sender: Sender = mockk(relaxed = true) {
            coEvery { send(any()) } returns SendingResult.SUCCESS
        }
        val store = TestStore()
        Loco.start(
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
        Loco.send(
            ClickLog(1, "jack")
        )
        Loco.send(
            ClickLog(2, "jill")
        )
        Loco.send(
            ClickLog(3, "desuger")
        )

        assertThat(store.storage.size).isEqualTo(3)

        advanceTimeBy(6000)

        Loco.stop()
        coVerify { sender.send(any()) }
        assertThat(store.storage.size).isEqualTo(0)
    }

    @Test
    fun sendFailed() = runBlockingTest {
        val smasher: Smasher = mockk(relaxed = true)
        val sender: Sender = mockk(relaxed = true) {
            coEvery { send(any()) } returns SendingResult.FAILED
        }
        val store = spyk(TestStore())
        Loco.start(
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
        Loco.send(
            ClickLog(1, "jack")
        )
        Loco.send(
            ClickLog(2, "jill")
        )
        Loco.send(
            ClickLog(3, "desuger")
        )

        assertThat(store.storage.size).isEqualTo(3)

        advanceTimeBy(6000)

        Loco.stop()
        coVerify { sender.send(any()) }
        coVerify { store.delete(any()) }
        assertThat(store.storage.size).isEqualTo(0)
    }

    @Test
    fun sendRetry() = runBlockingTest {
        val smasher: Smasher = mockk(relaxed = true)
        val sender: Sender = mockk(relaxed = true) {
            coEvery { send(any()) } returns SendingResult.RETRY
        }
        val store = spyk(TestStore())
        Loco.start(
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
        Loco.send(
            ClickLog(1, "jack")
        )
        Loco.send(
            ClickLog(2, "jill")
        )
        Loco.send(
            ClickLog(3, "desuger")
        )

        assertThat(store.storage.size).isEqualTo(3)

        advanceTimeBy(6000)

        Loco.stop()
        coVerify { sender.send(any()) }
        coVerify(exactly = 0) {
            store.delete(any())
        }
        assertThat(store.storage.size).isEqualTo(3)
    }

    @Test
    fun customBulkSize() = runBlockingTest {
        val smasher: Smasher = mockk(relaxed = true)
        val sender: Sender = mockk(relaxed = true) {
            coEvery { send(any()) } returns SendingResult.SUCCESS
        }
        val store = TestStore()
        Loco.start(
            LocoConfig(
                store = store,
                smashers = listOf(
                    smasher
                ),
                senders = listOf(
                    sender
                ),
                sendingBulkSize = 5
            ) {
                logToSmasher[smasher::class] = listOf(ClickLog::class)
                logToSender[sender::class] = listOf(ClickLog::class)
            },
            this
        )
        repeat(0.until(100).count()) {
            Loco.send(
                ClickLog(1, "jack")
            )
        }

        assertThat(store.storage.size).isEqualTo(100)

        advanceTimeBy(6000)

        Loco.stop()
        coVerify { sender.send(any()) }
        assertThat(store.storage.size).isEqualTo(95)
    }

    // TODO failed

    // TODO retry

    // TODO filter

    // TODO default sender

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

    class StdOutSender : Sender {
        override suspend fun send(logs: List<SmashedLog>): SendingResult {
            logs.forEach {
                println(it.toString())
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
