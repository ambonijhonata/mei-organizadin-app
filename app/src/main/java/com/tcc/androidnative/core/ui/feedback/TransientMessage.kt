package com.tcc.androidnative.core.ui.feedback

import androidx.annotation.StringRes

enum class MessageTone {
    SUCCESS,
    WARNING,
    ERROR,
    INFO
}

data class TransientMessage(
    val text: String? = null,
    @StringRes val textResId: Int? = null,
    val textArgs: List<String> = emptyList(),
    val tone: MessageTone,
    val durationMillis: Long
)

object MessageDurations {
    const val SHORT_3S: Long = 3_000L
    const val MEDIUM_5S: Long = 5_000L
    const val LONG_8S: Long = 8_000L
    const val LOGIN_ERROR_5_MIN: Long = 300_000L
}
