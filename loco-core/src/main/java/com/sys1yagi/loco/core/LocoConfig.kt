package com.sys1yagi.loco.core

import kotlin.reflect.KClass

class LocoConfig(
    val store: Store,
    val smasher: Smasher,
    val senders: Map<Sender, List<KClass<out LocoLog>>>,
    val scheduler: SendingScheduler,
    val extra: Extra = Extra()
) {
    data class Extra(
        val defaultSender: Sender? = null,
        val sendingBulkSize: Int = 10,
        val internist: Internist? = null
    )
}
