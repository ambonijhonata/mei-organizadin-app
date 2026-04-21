package com.tcc.androidnative.core.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object DateFormats {
    private val uiFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val apiFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun toApiDate(date: LocalDate): String = date.format(apiFormatter)

    fun parseApiDate(date: String): LocalDate = LocalDate.parse(date, apiFormatter)

    fun toUiDate(date: LocalDate): String = date.format(uiFormatter)

    fun parseUiDate(date: String): LocalDate = LocalDate.parse(date, uiFormatter)

    fun parseInstant(value: String): Instant = Instant.parse(value)

    fun instantToUiDate(value: Instant): String = value.atZone(ZoneOffset.UTC).toLocalDate().format(uiFormatter)

    fun toUtcMillis(date: LocalDate): Long = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    fun fromUtcMillis(utcMillis: Long): LocalDate = Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()
}
