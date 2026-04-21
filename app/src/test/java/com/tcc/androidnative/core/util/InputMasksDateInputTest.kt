package com.tcc.androidnative.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class InputMasksDateInputTest {
    @Test
    fun `format birth date input should apply ddMMyyyy mask without drift`() {
        assertEquals("26/12/2026", InputMasks.formatBirthDateInput("26122026"))
    }

    @Test
    fun `format birth date input should ignore non digits and keep order`() {
        assertEquals("26/12/2026", InputMasks.formatBirthDateInput("2a/6b-1_2@2#0$2%6"))
    }

    @Test
    fun `format birth date input should sanitize pasted date`() {
        assertEquals("26/12/2026", InputMasks.formatBirthDateInput("26-12-2026"))
    }

    @Test
    fun `format birth date input should truncate overflow digits`() {
        assertEquals("26/12/2026", InputMasks.formatBirthDateInput("26122026123"))
    }

    @Test
    fun `format birth date input should recompute mask after deletion`() {
        assertEquals("26/12/202", InputMasks.formatBirthDateInput("2612202"))
    }
}
