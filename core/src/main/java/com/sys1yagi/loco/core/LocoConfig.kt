package com.sys1yagi.loco.core

import kotlin.reflect.KClass

class LocoConfig(
    val store: Store,
    val smashers: List<Smasher>,
    val senders: List<Sender>,
    mapper: Mapper.() -> Unit
) {
    val mapping = Mapper().apply {
        mapper()
    }

    class Mapper {
        val logToSmasher = hashMapOf<KClass<out Smasher>, List<KClass<out LocoLog>>>()
        val logToSender = hashMapOf<KClass<out Sender>, List<KClass<out LocoLog>>>()
        var defaultSender: Sender? = null

        // logとsmasher
        // logとsender
        // logとfilter
    }
}
