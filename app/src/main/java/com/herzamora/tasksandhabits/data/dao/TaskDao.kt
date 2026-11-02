package com.herzamora.tasksandhabits.data.dao
import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import com.herzamora.tasksandhabits.data.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    // ---------- Writes ----------

    /** Returns the generated id of the new task. */
    @Insert
    suspend fun insert(task: TaskEntity): Long

    /** Replaces the whole row (used by the edit screen). */
    @Update
    suspend fun update(task: TaskEntity)

    @Query("UPDATE tasks SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun setCompleted(id: Long, isCompleted: Boolean)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long)

    // ---------- Reads ----------

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): TaskEntity?

    /** Due today or later, soonest first. Completed tasks sink within each day. */
    @Query(
        """
        SELECT * FROM tasks
        WHERE dueDate >= :today
        ORDER BY dueDate ASC, isCompleted ASC, createdAt ASC
        """
    )
    fun observeDueFrom(today: String): Flow<List<TaskEntity>>

    /** Pending tasks whose due date has already passed. */
    @Query(
        """
        SELECT * FROM tasks
        WHERE dueDate < :today AND isCompleted = 0
        ORDER BY dueDate ASC, createdAt ASC
        """
    )
    fun observeOverdue(today: String): Flow<List<TaskEntity>>

    /** Tasks without a due date. */
    @Query(
        """
        SELECT * FROM tasks
        WHERE dueDate IS NULL
        ORDER BY isCompleted ASC, createdAt ASC
        """
    )
    fun observeUndated(): Flow<List<TaskEntity>>
}
