package com.tcc.androidnative.core.util

object CpfValidator {
    fun isValid(input: String): Boolean {
        val digits = input.filter(Char::isDigit)
        if (digits.length != 11) return false
        if (digits.all { it == digits.first() }) return false

        val firstCheck = calculateCheckDigit(digits.substring(0, 9), 10)
        val secondCheck = calculateCheckDigit(digits.substring(0, 10), 11)

        return digits[9].digitToInt() == firstCheck && digits[10].digitToInt() == secondCheck
    }

    private fun calculateCheckDigit(base: String, weightStart: Int): Int {
        val sum = base.mapIndexed { index, c ->
            c.digitToInt() * (weightStart - index)
        }.sum()
        val remainder = sum % 11
        return if (remainder < 2) 0 else 11 - remainder
    }
}

