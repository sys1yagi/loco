package com.sys1yagi.loco.core

enum class SendingResult {
    SUCCESS, // consume log record
    FAILED, // consume log record
    RETRY // not consume log record
}
