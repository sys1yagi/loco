package com.sys1yagi.loco.store.android.sqlite.database

data class Record(
    val id: Long,
    val logTypeName: String,
    val smasherTypeName: String,
    val senderTypeName: String,
    val smashedLog: String
)
