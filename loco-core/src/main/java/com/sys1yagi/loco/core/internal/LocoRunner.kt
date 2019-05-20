package com.sys1yagi.loco.core.internal

import com.sys1yagi.loco.core.LocoConfig
import com.sys1yagi.loco.core.LocoLog
import com.sys1yagi.loco.core.Sender
import com.sys1yagi.loco.core.SendingResult
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach

class LocoRunner(val config: LocoConfig) {
    sealed class Event {
        data class Store(val log: SmashedLog) : Event()
        data class Send(val config: LocoConfig) : Event()
    }

    private var channel: Channel<Event>? = null

    private var mainJob: Job? = null

    private var waitForNextSendingJob: Job? = null

    @ObsoleteCoroutinesApi
    fun start(
        coroutineScope: CoroutineScope = GlobalScope
    ) {
        requireNotInitialized()

        channel = Channel(Channel.UNLIMITED)
        mainJob = coroutineScope.launch {
            channel?.consumeEach { event ->
                when (event) {
                    is Event.Store -> {
                        store(event.log, config)
                    }
                    is Event.Send -> {
                        sending(coroutineScope, event.config)
                    }
                }
            }
        }
        channel?.offer(Event.Send(config))
    }

    fun stop() {
        channel?.close()
        mainJob?.cancel()
        mainJob = null
    }

    fun send(log: LocoLog) {
        config.extra.internist?.onSend(log, config)

        val smasher = config.smasher
        val senders = findSenderTypeWithLocoLog(log, config)
        val smashed = smasher.smash(log)
        senders.forEach { sender ->
            val smashedLog = SmashedLog(
                log::class.java.name,
                smasher::class.java.name,
                sender::class.java.name,
                smashed
            )
            channel?.offer(Event.Store(smashedLog))
        }
    }

    private suspend fun store(log: SmashedLog, config: LocoConfig) {
        config.extra.internist?.onStore(log, config)

        config.store.store(log)
    }

    private suspend fun sending(scope: CoroutineScope, config: LocoConfig) {
        config.extra.internist?.onStartSending()

        val logs = config.store.load(config.extra.sendingBulkSize)
        val sendingResults = logs.groupBy { it.senderTypeName }.map { entry ->
            val senderTypeName = entry.key
            val senderTypedLogs = entry.value
            val sender = findSender(senderTypeName, config)

            config.extra.internist?.onSending(sender, senderTypedLogs, config)

            when (val result = sender.send(senderTypedLogs)) {
                SendingResult.SUCCESS, SendingResult.FAILED -> {
                    config.store.delete(senderTypedLogs)
                    Pair(sender, result)
                }
                SendingResult.RETRY -> {
                    // no op
                    Pair(sender, result)
                }
            }
        }

        config.extra.internist?.onEndSending(sendingResults, config)

        waitForNextSendingJob?.cancel()
        waitForNextSendingJob = scope.launch {
            config.scheduler.schedule(sendingResults, config) {
                channel?.offer(Event.Send(config))
            }
        }
    }

    private fun findSender(senderTypeName: String, config: LocoConfig): Sender {
        val senderType = Class.forName(senderTypeName)::kotlin.get()
        return config.senders.keys.find {
            it::class.java == senderType.java
        } ?: throw IllegalStateException(
            """
                    Missing Sender.
                    Add ${senderType.java.name} to LocoConfig#senders.
                    """.trimIndent()
        )
    }

    private fun findSenderTypeWithLocoLog(log: LocoLog, config: LocoConfig): List<Sender> {
        val klasses = config.senders.entries.filter { map ->
            map.value.any { it == log::class }
        }.map { it.key }
        if (klasses.isEmpty()) {
            throw IllegalStateException(
                """
                    Missing mapping to Sender Type.
                    Set up the mapping of ${log::class.java.name} class.
                    """.trimIndent()
            )
        }
        return klasses
    }

    private fun requireNotInitialized() {
        if (channel != null) {
            throw IllegalStateException(
                """
                    LocoRunner is already started.
                    """.trimIndent()
            )
        }
    }
}
