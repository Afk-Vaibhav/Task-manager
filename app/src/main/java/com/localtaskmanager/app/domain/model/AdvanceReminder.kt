package com.localtaskmanager.app.domain.model

enum class AdvanceReminder(val minutesBefore: Int, val label: String) {
    AT_TIME(0, "At time of task"),
    MINUTES_5(5, "5 minutes before"),
    MINUTES_10(10, "10 minutes before"),
    MINUTES_15(15, "15 minutes before"),
    MINUTES_30(30, "30 minutes before"),
    HOURS_1(60, "1 hour before");

    companion object {
        fun fromMinutes(minutes: Int?): AdvanceReminder {
            if (minutes == null) return AT_TIME
            return entries.firstOrNull { it.minutesBefore == minutes } ?: AT_TIME
        }
    }
}
