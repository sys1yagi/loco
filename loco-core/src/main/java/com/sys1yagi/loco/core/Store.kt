package com.sys1yagi.loco.core

import com.sys1yagi.loco.core.internal.SmashedLog

interface Store {
    suspend fun store(log: SmashedLog)
    suspend fun load(size: Int): List<SmashedLog>
    suspend fun delete(logs: List<SmashedLog>)
}
