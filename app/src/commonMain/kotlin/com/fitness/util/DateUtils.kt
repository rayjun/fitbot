package com.fitness.util

import kotlinx.datetime.*

object DateUtils {
    /** 
     * 返回相对于给定日期 [today] 偏移 [weekOffset] 周的周一。
     * dayOfWeek.ordinal: Mon=0 ... Sun=6
     */
    fun getMondayOfWeek(today: LocalDate, weekOffset: Int): LocalDate {
        val daysSinceMonday = today.dayOfWeek.ordinal
        val thisMonday = today.minus(daysSinceMonday, DateTimeUnit.DAY)
        return thisMonday.plus(weekOffset * 7, DateTimeUnit.DAY)
    }

    /** 获取今天的日期字符串 YYYY-MM-DD */
    fun getTodayString(): String {
        return Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
    }
}
