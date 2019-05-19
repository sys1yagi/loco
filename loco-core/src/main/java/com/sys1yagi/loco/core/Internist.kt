package com.sys1yagi.loco.core

import com.sys1yagi.loco.core.internal.SmashedLog

interface Internist {
    fun onSend(locoLog: LocoLog, config: LocoConfig)
    fun onStore(log: SmashedLog, config: LocoConfig)
    fun onStartSending()
    fun onSending(sender: Sender, logs: List<SmashedLog>, config: LocoConfig)
    fun onEndSending(sendingResults: List<Pair<Sender, SendingResult>>, config: LocoConfig)
}
