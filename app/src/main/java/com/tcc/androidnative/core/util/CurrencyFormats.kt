package com.tcc.androidnative.core.util

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

object CurrencyFormats {
    private val locale: Locale = Locale("pt", "BR")

    fun formatForUi(value: BigDecimal): String {
        val formatter = NumberFormat.getCurrencyInstance(locale)
        return formatter.format(value)
    }

    fun parseUiValue(input: String): BigDecimal {
        val normalized = input
            .replace("R$", "")
            .replace(".", "")
            .replace(",", ".")
            .trim()
        return normalized.toBigDecimalOrNull() ?: BigDecimal.ZERO
    }
}

