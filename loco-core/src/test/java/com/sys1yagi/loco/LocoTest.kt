package com.sys1yagi.loco

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import com.sys1yagi.loco.core.Internist
import com.sys1yagi.loco.core.Loco
import com.sys1yagi.loco.core.LocoConfig
import com.sys1yagi.loco.core.LocoLog
import com.sys1yagi.loco.core.Sender
import com.sys1yagi.loco.core.SendingResult
import com.sys1yagi.loco.core.SendingScheduler
import com.sys1yagi.loco.core.Smasher
import com.sys1yagi.loco.core.Store
import com.sys1yagi.loco.core.internal.SmashedLog
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@ExperimentalCoroutinesApi
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
                store = InMemoryStore(),
                smasher = GsonSmasher(Gson()),
                senders = mapOf(
                    StdOutSender() to listOf(
                        ClickLog::class
                    )
                ),
                scheduler = IntervalSendingScheduler(5000L)
            )
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
                store = InMemoryStore(),
                smasher = GsonSmasher(Gson()),
                senders = mapOf(
                    StdOutSender() to listOf(
                        ClickLog::class
                    )
                ),
                scheduler = IntervalSendingScheduler(5000L)
            )
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
                store = InMemoryStore(),
                smasher = smasher,
                senders = mapOf(
                    StdOutSender() to listOf(
                        ClickLog::class
                    )
                ),
                scheduler = IntervalSendingScheduler(5000L)
            )
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
                store = InMemoryStore(),
                smasher = smasher,
                senders = mapOf(
                    sender to emptyList()
                ),
                scheduler = IntervalSendingScheduler(5000L)
            )
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
                smasher = smasher,
                senders = mapOf(
                    sender to listOf(ClickLog::class)
                ),
                scheduler = IntervalSendingScheduler(5000L)
            ),
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
        val store = InMemoryStore()
        Loco.start(
            LocoConfig(
                store = store,
                smasher = smasher,
                senders = mapOf(
                    sender to listOf(ClickLog::class)
                ),
                scheduler = IntervalSendingScheduler(5000L)
            ),
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
        val store = spyk(InMemoryStore())
        Loco.start(
            LocoConfig(
                store = store,
                smasher = smasher,
                senders = mapOf(
                    sender to listOf(ClickLog::class)
                ),
                scheduler = IntervalSendingScheduler(5000L)
            ),
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
        val store = spyk(InMemoryStore())
        Loco.start(
            LocoConfig(
                store = store,
                smasher = smasher,
                senders = mapOf(
                    sender to listOf(ClickLog::class)
                ),
                scheduler = IntervalSendingScheduler(5000L)
            ),
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
        val store = InMemoryStore()
        Loco.start(
            LocoConfig(
                store = store,
                smasher = smasher,
                senders = mapOf(
                    sender to listOf(ClickLog::class)
                ),
                scheduler = IntervalSendingScheduler(5000L),
                extra = LocoConfig.Extra(
                    sendingBulkSize = 5
                )
            ),
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

    @Test
    fun defaultSender() = runBlockingTest {
        val smasher: Smasher = mockk(relaxed = true)
        val sender: Sender = spyk(StdOutSender())

        Loco.start(
            LocoConfig(
                store = InMemoryStore(),
                smasher = smasher,
                senders = mapOf(),
                scheduler = IntervalSendingScheduler(5000L),
                extra = LocoConfig.Extra(
                    defaultSender = sender,
                    internist = PrintInternist()
                )
            ),
            this
        )
        Loco.send(
            ClickLog(1, "jack")
        )

        advanceTimeBy(6000)

        Loco.stop()

        coVerify { sender.send(any()) }
    }

    @Test
    fun multiSender() = runBlockingTest {
        val smasher: Smasher = mockk(relaxed = true)
        val sender1: Sender = spyk(StdOutSender())
        val sender2: Sender = spyk(FileSender())

        val store = InMemoryStore()
        Loco.start(
            LocoConfig(
                store = store,
                smasher = smasher,
                senders = mapOf(
                    sender1 to listOf(ClickLog::class),
                    sender2 to listOf(ClickLog::class)
                ),
                scheduler = IntervalSendingScheduler(5000L),
                extra = LocoConfig.Extra(
                    internist = PrintInternist()
                )
            ),
            this
        )

        Loco.send(
            ClickLog(1, "jack")
        )

        advanceTimeBy(6000)

        Loco.stop()
        coVerify { sender1.send(any()) }
        coVerify { sender2.send(any()) }
    }

    data class ClickLog(
        val id: Int,
        val name: String
    ) : LocoLog

    class GsonSmasher(val gson: Gson) : Smasher {
        override fun smash(log: LocoLog): String {
            return gson.toJson(log)
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

    class FileSender : Sender {
        override suspend fun send(logs: List<SmashedLog>): SendingResult {
            logs.forEach {
                println(it.toString())
            }
            return SendingResult.SUCCESS
        }
    }

    class IntervalSendingScheduler(private val interval: Long) : SendingScheduler {
        override suspend fun schedule(
            latestResults: List<Pair<Sender, SendingResult>>,
            config: LocoConfig,
            offer: () -> Unit
        ) {
            delay(interval)
            offer()
        }
    }

    class InMemoryStore : Store {
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

    class PrintInternist : Internist {
        override fun onSend(locoLog: LocoLog, config: LocoConfig) {
            println("onSend: ${Thread.currentThread().id}")
        }

        override fun onStoreOffer(log: SmashedLog, config: LocoConfig) {
            println("onStoreOffer: ${Thread.currentThread().id}")
        }

        override fun onStore(log: SmashedLog, config: LocoConfig) {
            println("onStore : ${Thread.currentThread().id}")
        }

        override fun onStartSending() {
            println("onStartSending : ${Thread.currentThread().id}")
        }

        override fun onSending(sender: Sender, logs: List<SmashedLog>, config: LocoConfig) {
            println("onSending: $sender, ${logs.size}, : ${Thread.currentThread().id}")
        }

        override fun onEndSending(
            sendingResults: List<Pair<Sender, SendingResult>>,
            config: LocoConfig
        ) {
            println("onEndSending : ${Thread.currentThread().id}")
        }

    }
}
