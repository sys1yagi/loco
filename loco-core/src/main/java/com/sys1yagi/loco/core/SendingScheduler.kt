package com.sys1yagi.loco.core

interface SendingScheduler {
    suspend fun schedule(
        latestResults: List<Pair<Sender, SendingResult>>,
        config: LocoConfig,
        offer: () -> Unit
    )
}
