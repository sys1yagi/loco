package com.sys1yagi.loco.core

import kotlin.reflect.KClass

class LocoConfig(
    val store: Store,
    val smasher: Smasher,
    val senders: List<Sender>,
    val scheduler: SendingScheduler,
    val sendingBulkSize: Int = 10,
    mapper: Mapper.() -> Unit
) {
    val mapping = Mapper().apply {
        mapper()
    }

    class Mapper {
        val logToSender = hashMapOf<KClass<out Sender>, List<KClass<out LocoLog>>>()
        var defaultSender: Sender? = null // TODO
    }
}
