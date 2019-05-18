package com.sys1yagi.loco.core

import com.sys1yagi.loco.core.internal.SmashedLog

interface Store {
    fun store(log: SmashedLog)
    fun load(size: Int): List<SmashedLog>
    fun delete(logs: List<SmashedLog>)
}
