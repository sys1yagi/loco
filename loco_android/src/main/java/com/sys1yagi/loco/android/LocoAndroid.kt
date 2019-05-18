package com.sys1yagi.loco.android

import com.sys1yagi.loco.core.LocoConfig
import com.sys1yagi.loco.core.LocoLog
import com.sys1yagi.loco.core.Sender
import com.sys1yagi.loco.core.Smasher
import com.sys1yagi.loco.core.internal.SmashedLog
import kotlin.reflect.KClass

object LocoAndroid {

    var config: LocoConfig? = null

    fun start(config: LocoConfig) {
        this.config = config
        // TODO
        // start receive channel
    }

    fun stop() {
        this.config = null
    }

    fun send(log: LocoLog) {
        val config = requireInitialized()
        val smasher = findSmasher(log, config)
        val senderType = findSenderType(log, config)
        val smashed = smasher.smash(log)
        val smashedLog = SmashedLog(
            log::class.java.name,
            smasher::class.java.name,
            senderType.java.name,
            smashed
        )
        config.store.store(smashedLog)
    }

    private fun findSender(log: LocoLog, config: LocoConfig): Sender {
        val senderType = findSenderType(log, config)
        return config.senders.find {
            it::class.java == senderType.java
        } ?: throw IllegalStateException(
            """
                    Missing Smasher.
                    Add ${senderType.java.name} to LocoConfig#smashers.
                    """.trimIndent()
        )
    }

    private fun findSenderType(log: LocoLog, config: LocoConfig): KClass<out Sender> {
        return config.mapping.logToSender.entries.find { map ->
            map.value.any { it == log::class }
        }?.key ?: throw IllegalStateException(
            """
                    Missing mapping to Sender Type.
                    Set up the mapping of ${log::class.java.name} class.
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
        return this.config ?: throw IllegalStateException(
            """
                    Loco is not yet initialized.
                    Please call start(config: LocoConfig) first.
                    """.trimIndent()
        )
    }

}

