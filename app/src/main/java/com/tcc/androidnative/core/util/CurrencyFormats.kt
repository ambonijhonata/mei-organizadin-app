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

    fun formatInput(value: String): String {
        val digits = value.filter(Char::isDigit)
        if (digits.isEmpty()) return ""

        val normalizedDigits = digits.trimStart('0').ifEmpty { "0" }
        val decimalValue = normalizedDigits.toBigDecimal().movePointLeft(2)
        return formatForUi(decimalValue)
    }

    fun parseUiValue(input: String): BigDecimal {
        val digits = input.filter(Char::isDigit)
        if (digits.isEmpty()) return BigDecimal.ZERO
        return digits.toBigDecimal().movePointLeft(2)
    }
}
