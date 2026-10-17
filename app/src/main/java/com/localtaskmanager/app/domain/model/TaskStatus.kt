package com.localtaskmanager.app.domain.model

enum class TaskStatus(val value: String) {
    OPEN("OPEN"),
    COMPLETED("COMPLETED");

    companion object {
        fun fromValue(value: String): TaskStatus {
            return entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: OPEN
        }
    }
}
