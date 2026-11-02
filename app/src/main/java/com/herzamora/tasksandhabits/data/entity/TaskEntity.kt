package com.herzamora.tasksandhabits.data.entity
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String? = null,
    val isCompleted: Boolean = false,
    /** Due date as ISO-8601 text, "YYYY-MM-DD". Date only: no time, no time zone. */
    val dueDate: String? = null,
    /** Exact reminder instant, in epoch milliseconds (UTC). */
    val reminderAt: Long? = null,
    /** Creation instant, in epoch milliseconds (UTC). */
    val createdAt: Long,
)