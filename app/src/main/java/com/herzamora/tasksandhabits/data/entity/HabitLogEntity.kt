package com.herzamora.tasksandhabits.data.entity
import androidx.room3.Entity
import androidx.room3.ForeignKey

/** One row = the habit was done on that date. No row = not done. */
@Entity(
    tableName = "habit_logs",
    primaryKeys = ["habitId", "date"],
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class HabitLogEntity(
    val habitId: Long,
    /** Local calendar date as ISO-8601 text, "YYYY-MM-DD". */
    val date: String,
)