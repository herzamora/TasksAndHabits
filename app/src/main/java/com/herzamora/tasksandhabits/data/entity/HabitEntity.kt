package com.herzamora.tasksandhabits.data.entity
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    /** Archived habits are hidden but keep their history. */
    val isArchived: Boolean = false,
    /** Creation instant, in epoch milliseconds (UTC). Days before it are never "missed". */
    val createdAt: Long,
)