package com.sys1yagi.loco.core.internal

import com.sys1yagi.loco.core.LocoConfig
import com.sys1yagi.loco.core.LocoLog
import com.sys1yagi.loco.core.Sender
import com.sys1yagi.loco.core.SendingResult
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlin.reflect.KClass

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
                        config.store.store(event.log)
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
        mainJob?.cancel()
        mainJob = null
    }

    fun send(log: LocoLog) {
        val smasher = config.smasher
        val senderType = findSenderTypeWithLocoLog(log, config)
        val smashed = smasher.smash(log)
        val smashedLog = SmashedLog(
            log::class.java.name,
            smasher::class.java.name,
            senderType.java.name,
            smashed
        )
        channel?.offer(Event.Store(smashedLog))
    }

    private suspend fun sending(scope: CoroutineScope, config: LocoConfig) {
        val logs = config.store.load(config.sendingBulkSize)
        val sendingResults = logs.groupBy { it.senderTypeName }.map { entry ->
            val senderTypeName = entry.key
            val senderTypedLogs = entry.value
            val sender = findSender(senderTypeName, config)

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

        waitForNextSendingJob?.cancel()
        waitForNextSendingJob = scope.launch {
            config.scheduler.schedule(sendingResults, config) {
                channel?.offer(Event.Send(config))
            }
        }
    }

    private fun findSender(senderTypeName: String, config: LocoConfig): Sender {
        val senderType = Class.forName(senderTypeName)::kotlin.get()
        return config.senders.find {
            it::class.java == senderType.java
        } ?: throw IllegalStateException(
            """
                    Missing Sender.
                    Add ${senderType.java.name} to LocoConfig#senders.
                    """.trimIndent()
        )
    }

    private fun findSenderTypeWithLocoLog(log: LocoLog, config: LocoConfig): KClass<out Sender> {
        return config.mapping.logToSender.entries.find { map ->
            map.value.any { it == log::class }
        }?.key ?: throw IllegalStateException(
            """
                    Missing mapping to Sender Type.
                    Set up the mapping of ${log::class.java.name} class.
                    """.trimIndent()
        )
    }

    private fun findSenderTypeWithKClass(klass: KClass<out LocoLog>, config: LocoConfig): KClass<out Sender> {
        return config.mapping.logToSender.entries.find { map ->
            map.value.any { it == klass }
        }?.key ?: throw IllegalStateException(
            """
                    Missing mapping to Sender Type.
                    Set up the mapping of ${klass::class.java.name} class.
                    """.trimIndent()
        )
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
