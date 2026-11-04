package com.tcc.androidnative.feature.calendar.ui

import com.tcc.androidnative.feature.calendar.data.CalendarPaymentStatus
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test

class CalendarHomeSlotMapTest {
    private val calendarZone = ZoneId.of("America/Sao_Paulo")

    @Test
    fun `thirty minute event occupies only its initial slot`() {
        val event = agendaItem("2026-11-05T00:00:00Z", "2026-11-05T00:30:00Z")

        val slots = buildSlotMap(listOf(event), calendarZone)

        assertEquals(setOf("21:00"), slots.keys)
        assertFalse(slots.containsKey("21:30"))
    }

    @Test
    fun `one hour event occupies two consecutive slots and leaves adjacent slots free`() {
        val event = agendaItem("2026-11-05T00:30:00Z", "2026-11-05T01:30:00Z")

        val slots = buildSlotMap(listOf(event), calendarZone)

        assertEquals(setOf("21:30", "22:00"), slots.keys)
        assertSame(event, slots["21:30"])
        assertSame(event, slots["22:00"])
        assertFalse(slots.containsKey("21:00"))
        assertFalse(slots.containsKey("22:30"))
    }

    @Test
    fun `one hour event produces one visual row spanning two slots`() {
        val event = agendaItem("2026-11-04T20:00:00Z", "2026-11-04T21:00:00Z", "17:00")
        val timeSlots = listOf(
            LocalTime.of(16, 30),
            LocalTime.of(17, 0),
            LocalTime.of(17, 30),
            LocalTime.of(18, 0)
        )

        val rows = buildAgendaRows(timeSlots, listOf(event), calendarZone)

        assertEquals(3, rows.size)
        assertEquals(LocalTime.of(16, 30), rows[0].slot)
        assertEquals(LocalTime.of(17, 0), rows[1].slot)
        assertSame(event, rows[1].item)
        assertEquals(2, rows[1].occupiedSlotCount)
        assertEquals(LocalTime.of(18, 0), rows[2].slot)
        assertFalse(rows.any { it.slot == LocalTime.of(17, 30) })
    }

    @Test
    fun `time range label provides start and end only when an end exists`() {
        assertEquals("17:00" to "18:00", splitTimeRangeLabel("17:00 - 18:00"))
        assertEquals("17:00" to null, splitTimeRangeLabel("17:00"))
    }

    private fun agendaItem(start: String, end: String, slotLabel: String = "unused"): CalendarAgendaItem =
        CalendarAgendaItem(
            eventId = 1L,
            eventStart = Instant.parse(start),
            eventEnd = Instant.parse(end),
            slotLabel = slotLabel,
            timeLabel = "unused",
            title = "Evento",
            serviceDescription = null,
            durationLabel = "unused",
            serviceTotalValue = BigDecimal.ZERO,
            paymentStatus = CalendarPaymentStatus.NONE
        )
}
