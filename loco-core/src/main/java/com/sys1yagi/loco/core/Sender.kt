package com.sys1yagi.loco.core

import com.sys1yagi.loco.core.internal.SmashedLog

interface Sender {

    suspend fun send(logs: List<SmashedLog>): SendingResult
}
