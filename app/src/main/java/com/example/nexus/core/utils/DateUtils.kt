package com.example.nexus.core.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Utility functions for date/time formatting in chat context.
 */
object DateUtils {

    /**
     * Formats timestamp to a human-readable chat time format.
     * - Today: "14:30"
     * - Yesterday: "Hôm qua"
     * - This week: "Thứ 2" (day name)
     * - Older: "15/04/2025"
     */
    fun formatChatTime(timestamp: Long): String {
        val now = Calendar.getInstance()
        val messageTime = Calendar.getInstance().apply { timeInMillis = timestamp }

        return when {
            isSameDay(now, messageTime) -> {
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
            }
            isYesterday(now, messageTime) -> "Hôm qua"
            isSameWeek(now, messageTime) -> {
                val dayNames = arrayOf("CN", "Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7")
                dayNames[messageTime.get(Calendar.DAY_OF_WEEK) - 1]
            }
            else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }

    /**
     * Formats timestamp for message bubble display.
     * Always shows time (HH:mm)
     */
    fun formatMessageTime(timestamp: Long): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
    }

    /**
     * Formats for message date separator in chat.
     * - Today: "Hôm nay"
     * - Yesterday: "Hôm qua"
     * - This year: "15 tháng 4"
     * - Other: "15 tháng 4, 2024"
     */
    fun formatDateSeparator(timestamp: Long): String {
        val now = Calendar.getInstance()
        val messageTime = Calendar.getInstance().apply { timeInMillis = timestamp }

        return when {
            isSameDay(now, messageTime) -> "Hôm nay"
            isYesterday(now, messageTime) -> "Hôm qua"
            now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR) -> {
                val day = messageTime.get(Calendar.DAY_OF_MONTH)
                val month = messageTime.get(Calendar.MONTH) + 1
                "$day tháng $month"
            }
            else -> {
                SimpleDateFormat("dd 'tháng' MM, yyyy", Locale.getDefault()).format(Date(timestamp))
            }
        }
    }

    /**
     * Formats "last seen" timestamp.
     * - Just now (< 1 min): "Vừa mới truy cập"
     * - Minutes ago: "5 phút trước"
     * - Hours ago: "2 giờ trước"
     * - Today: "Truy cập lúc 14:30"
     * - Yesterday: "Truy cập hôm qua"
     * - Older: "Truy cập 15/04"
     */
    fun formatLastSeen(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)

        return when {
            minutes < 1 -> "Vừa mới truy cập"
            minutes < 60 -> "$minutes phút trước"
            hours < 24 -> "$hours giờ trước"
            hours < 48 -> "Truy cập hôm qua"
            else -> {
                val formatted = SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(timestamp))
                "Truy cập $formatted"
            }
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                && cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(now: Calendar, other: Calendar): Boolean {
        val yesterday = now.clone() as Calendar
        yesterday.add(Calendar.DAY_OF_YEAR, -1)
        return isSameDay(yesterday, other)
    }

    private fun isSameWeek(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                && cal1.get(Calendar.WEEK_OF_YEAR) == cal2.get(Calendar.WEEK_OF_YEAR)
    }
}
