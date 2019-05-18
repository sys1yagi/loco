package com.sys1yagi.loco.core

import com.sys1yagi.loco.core.internal.SmashedLog
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlin.reflect.KClass

object Loco {

    sealed class Event {
        data class Store(val log: SmashedLog) : Event()
        data class Send(val config: LocoConfig) : Event()
    }

    private var config: LocoConfig? = null

    private var channel: Channel<Event>? = null

    private var mainJob: Job? = null

    private var waitForNextSendingJob: Job? = null

    fun start(
        config: LocoConfig,
        coroutineScope: CoroutineScope = GlobalScope
    ) {
        requireNotInitialized()

        Loco.config = config
        channel = Channel(Channel.UNLIMITED)
        mainJob = coroutineScope.launch {
            channel?.consumeEach { event ->
                when (event) {
                    is Event.Store -> {
                        config.store.store(event.log)
                    }
                    is Event.Send -> {
                        send(coroutineScope, event.config)
                    }
                }
            }
        }
        channel?.offer(Loco.Event.Send(config))
    }

    fun stop() {
        config = null
        mainJob?.cancel()
        mainJob = null
    }

    fun send(log: LocoLog) {
        val config = requireInitialized()
        val smasher = findSmasher(log, config)
        val senderType = findSenderTypeWithLocoLog(log, config)
        val smashed = smasher.smash(log)
        val smashedLog = SmashedLog(
            log::class.java.name,
            smasher::class.java.name,
            senderType.java.name,
            smashed
        )
        channel?.offer(Loco.Event.Store(smashedLog))
    }

    private suspend fun send(scope: CoroutineScope, config: LocoConfig) {
        // TODO size config
        val logs = config.store.load(10)
        logs.groupBy { it.senderTypeName }.forEach { entry ->
            val senderTypeName = entry.key
            val senderTypedLogs = entry.value
            val sender = findSender(senderTypeName, config)

            when (sender.send(senderTypedLogs)) {
                SendingResult.SUCCESS, SendingResult.FAILED -> {
                    config.store.delete(senderTypedLogs)
                }
                SendingResult.RETRY -> {
                    // no op
                }
            }
        }

        waitForNextSendingJob?.cancel()
        waitForNextSendingJob = scope.launch {
            // TODO delay config
            delay(5000)
            channel?.offer(Loco.Event.Send(config))
        }
    }

    private fun findSender(senderTypeName: String, config: LocoConfig): Sender {
        val senderType = Class.forName(senderTypeName)::kotlin.get()
        // TODO type check
        return config.senders.find {
            it::class.java == senderType.java
        } ?: throw IllegalStateException(
            """
                    Missing Smasher.
                    Add ${senderType.java.name} to LocoConfig#smashers.
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

    private fun findSmasher(log: LocoLog, config: LocoConfig): Smasher {
        val smasherType = findSmasherType(log, config)
        return config.smashers.find {
            it::class.java == smasherType.java
        } ?: throw IllegalStateException(
            """
                    Missing Smasher.
                    Add ${smasherType.java.name} to LocoConfig#smashers.
                    """.trimIndent()
        )
    }

    private fun findSmasherType(log: LocoLog, config: LocoConfig): KClass<out Smasher> {
        return config.mapping.logToSmasher.entries.find { map ->
            map.value.any { it == log::class }
        }?.key ?: throw IllegalStateException(
            """
                    Missing mapping to Smasher Type.
                    Set up the mapping of ${log::class.java.name} class.
                    """.trimIndent()
        )
    }

    private fun requireInitialized(): LocoConfig {
        return config ?: throw IllegalStateException(
            """
                    Loco is not yet initialized.
                    Please call start(config: LocoConfig) first.
                    """.trimIndent()
        )
    }

    private fun requireNotInitialized() {
        if (this.config != null) {
            throw IllegalStateException(
                """
                    Loco is already initialized.
                    """.trimIndent()
            )
        }
    }
}

