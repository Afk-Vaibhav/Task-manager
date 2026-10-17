package com.localtaskmanager.app.domain.model

data class Category(
    val id: String,
    val name: String,
    val iconKey: String,
    val colorArgb: Int,
    val isDefault: Boolean = false,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)
