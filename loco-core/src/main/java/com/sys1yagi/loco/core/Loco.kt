package com.sys1yagi.loco.core

import com.sys1yagi.loco.core.internal.LocoRunner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope

object Loco {
    private var runner: LocoRunner? = null

    fun start(
        config: LocoConfig,
        coroutineScope: CoroutineScope = GlobalScope
    ) {
        requireNotInitialized()
        runner = LocoRunner(config).also {
            it.start(coroutineScope)
        }
    }

    fun stop() {
        runner?.stop()
        runner = null
    }

    fun send(log: LocoLog) {
        requireInitialized()
        runner?.send(log)
    }

    private fun requireInitialized() {
        runner ?: throw IllegalStateException(
            """
                    Loco is not yet initialized.
                    Please call start(config: LocoConfig) first.
                    """.trimIndent()
        )
    }

    private fun requireNotInitialized() {
        if (this.runner != null) {
            throw IllegalStateException(
                """
                    Loco is already initialized.
                    """.trimIndent()
            )
        }
    }
}

