package com.herzamora.tasksandhabits.data.dao
import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.herzamora.tasksandhabits.data.entity.HabitLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitLogDao {

    /** Idempotent: marking the same day twice keeps the existing row (returns -1). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(log: HabitLogEntity): Long

    /** Un-marks a day. Does nothing if there was no log. */
    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND date = :date")
    suspend fun delete(habitId: Long, date: String)

    /** Every log of every habit (the Habits screen derives status and streaks from this). */
    @Query("SELECT * FROM habit_logs")
    fun observeAll(): Flow<List<HabitLogEntity>>

    /** Logs of one habit between two dates, both included, oldest first. */
    @Query(
        """
        SELECT * FROM habit_logs
        WHERE habitId = :habitId AND date BETWEEN :startDate AND :endDate
        ORDER BY date ASC
        """
    )
    fun observeBetween(habitId: Long, startDate: String, endDate: String): Flow<List<HabitLogEntity>>
}
