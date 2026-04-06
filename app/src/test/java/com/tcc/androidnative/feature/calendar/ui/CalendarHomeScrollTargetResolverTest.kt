package com.tcc.androidnative.feature.calendar.ui

import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class CalendarHomeScrollTargetResolverTest {
    @Test
    fun `should choose first occupied slot when day has events`() {
        val slotLabels = buildSlots()
        val occupied = setOf("14:30", "16:00")

        val index = resolveInitialFocusIndex(
            slotLabels = slotLabels,
            occupiedSlotLabels = occupied,
            deviceLocalTime = LocalTime.of(9, 10)
        )

        assertEquals(29, index)
    }

    @Test
    fun `should fallback to floored device time when day has no events`() {
        val slotLabels = buildSlots()

        val index = resolveInitialFocusIndex(
            slotLabels = slotLabels,
            occupiedSlotLabels = emptySet(),
            deviceLocalTime = LocalTime.of(10, 40)
        )

        assertEquals(21, index)
    }

    @Test
    fun `should floor device minute to matching half-hour label`() {
        assertEquals("10:00", mapDeviceTimeToSlotLabel(LocalTime.of(10, 5)))
        assertEquals("10:30", mapDeviceTimeToSlotLabel(LocalTime.of(10, 59)))
    }

    @Test
    fun `first day of current month helper should use utc month start`() {
        val millis = firstDayOfCurrentMonthUtcMillis { java.time.LocalDate.of(2026, 4, 20) }
        assertEquals(java.time.LocalDate.of(2026, 4, 1), utcMillisToLocalDate(millis))
    }

    private fun buildSlots(): List<String> {
        return (0 until 48).map { index ->
            val hour = index / 2
            val minute = if (index % 2 == 0) 0 else 30
            "%02d:%02d".format(hour, minute)
        }
    }
}
