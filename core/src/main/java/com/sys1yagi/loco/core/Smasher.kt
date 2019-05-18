package com.sys1yagi.loco.core

/**
 * SmashedLog serializer
 */
interface Smasher {
    fun smash(log: LocoLog): String
    fun registerFilter(filter: Filter)
}
