package com.tcc.androidnative.core.network

import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException

internal fun Throwable.isLikelyTransientNetworkFailure(): Boolean {
    var current: Throwable? = this
    while (current != null) {
        if (current is SocketTimeoutException || current is InterruptedIOException || current is IOException) {
            return true
        }
        current = current.cause
    }
    return false
}
