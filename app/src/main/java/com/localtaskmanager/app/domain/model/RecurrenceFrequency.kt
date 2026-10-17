package com.localtaskmanager.app.domain.model

enum class RecurrenceFrequency(val value: String, val displayName: String) {
    DAILY("DAILY", "Daily"),
    WEEKDAYS("WEEKDAYS", "Selected Weekdays (Mon-Fri)"),
    WEEKLY("WEEKLY", "Weekly"),
    MONTHLY("MONTHLY", "Monthly");

    companion object {
        fun fromValue(value: String): RecurrenceFrequency {
            return entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: DAILY
        }
    }
}
