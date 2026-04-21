package com.tcc.androidnative.core.util

object InputMasks {
    fun digitsOnly(value: String): String = value.filter(Char::isDigit)

    fun formatCpfInput(value: String): String {
        val digits = digitsOnly(value).take(11)
        return when (digits.length) {
            0 -> ""
            in 1..3 -> digits
            in 4..6 -> "${digits.take(3)}.${digits.drop(3)}"
            in 7..9 -> "${digits.substring(0, 3)}.${digits.substring(3, 6)}.${digits.substring(6)}"
            else -> "${digits.substring(0, 3)}.${digits.substring(3, 6)}.${digits.substring(6, 9)}-${digits.substring(9)}"
        }
    }

    fun formatBirthDateInput(value: String): String {
        val digits = digitsOnly(value).take(8)
        return when (digits.length) {
            0 -> ""
            in 1..2 -> digits
            in 3..4 -> "${digits.take(2)}/${digits.drop(2)}"
            else -> "${digits.substring(0, 2)}/${digits.substring(2, 4)}/${digits.substring(4)}"
        }
    }

    fun formatPhoneInput(value: String): String {
        val digits = digitsOnly(value).take(11)
        return when (digits.length) {
            0 -> ""
            in 1..2 -> "(${digits}"
            in 3..7 -> "(${digits.take(2)}) ${digits.drop(2)}"
            else -> "(${digits.substring(0, 2)}) ${digits.substring(2, 7)}-${digits.substring(7)}"
        }
    }
}
