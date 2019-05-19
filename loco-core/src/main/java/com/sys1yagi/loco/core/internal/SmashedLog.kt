package com.sys1yagi.loco.core.internal

import kotlin.random.Random

class SmashedLog(
    val logTypeName: String,
    val smasherTypeName: String,
    val senderTypeName: String,
    val smashedLog: String,
    val uniqueId: Long = random()
) {
    companion object {
        val random = Random(System.currentTimeMillis())
        fun random(): Long {
            return random.nextLong()
        }
    }
}
