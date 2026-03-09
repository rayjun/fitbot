package com.fitness.util

import kotlinx.datetime.LocalDate
import kotlin.test.*

class DateUtilsTest {

    @Test
    fun testGetMondayOfWeek_OnMonday() {
        // 2024-03-04 is Monday
        val today = LocalDate(2024, 3, 4)
        val monday = DateUtils.getMondayOfWeek(today, 0)
        assertEquals(LocalDate(2024, 3, 4), monday)
    }

    @Test
    fun testGetMondayOfWeek_OnSunday() {
        // 2024-03-10 is Sunday
        val today = LocalDate(2024, 3, 10)
        val monday = DateUtils.getMondayOfWeek(today, 0)
        assertEquals(LocalDate(2024, 3, 4), monday)
    }

    @Test
    fun testGetMondayOfWeek_NextWeek() {
        // 2024-03-06 is Wednesday
        val today = LocalDate(2024, 3, 6)
        val nextMonday = DateUtils.getMondayOfWeek(today, 1)
        assertEquals(LocalDate(2024, 3, 11), nextMonday)
    }

    @Test
    fun testGetMondayOfWeek_PrevWeek() {
        // 2024-03-06 is Wednesday
        val today = LocalDate(2024, 3, 6)
        val prevMonday = DateUtils.getMondayOfWeek(today, -1)
        assertEquals(LocalDate(2024, 2, 26), prevMonday)
    }
}
