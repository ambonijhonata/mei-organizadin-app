package com.tcc.androidnative.core.ui.feedback

enum class MessageTone {
    SUCCESS,
    WARNING,
    ERROR,
    INFO
}

data class TransientMessage(
    val text: String,
    val tone: MessageTone,
    val durationMillis: Long
)

object MessageDurations {
    const val SHORT_3S: Long = 3_000L
    const val MEDIUM_5S: Long = 5_000L
    const val LONG_8S: Long = 8_000L
}

