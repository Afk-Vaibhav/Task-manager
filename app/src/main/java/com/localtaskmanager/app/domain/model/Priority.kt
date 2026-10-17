package com.localtaskmanager.app.domain.model

enum class Priority(val value: Int, val displayName: String) {
    LOW(0, "Low"),
    NORMAL(1, "Normal"),
    HIGH(2, "High");

    companion object {
        fun fromValue(value: Int): Priority {
            return entries.firstOrNull { it.value == value } ?: NORMAL
        }
    }
}
