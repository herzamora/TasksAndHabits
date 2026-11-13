package com.herzamora.tasksandhabits.data
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.herzamora.tasksandhabits.data.dao.HabitDao
import com.herzamora.tasksandhabits.data.dao.HabitLogDao
import com.herzamora.tasksandhabits.data.dao.TaskDao
import com.herzamora.tasksandhabits.data.entity.HabitEntity
import com.herzamora.tasksandhabits.data.entity.HabitLogEntity
import com.herzamora.tasksandhabits.data.entity.TaskEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DaoTest {

    private lateinit var db: AppDatabase
    private lateinit var taskDao: TaskDao
    private lateinit var habitDao: HabitDao
    private lateinit var logDao: HabitLogDao

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder<AppDatabase>(context)
            .setDriver(AndroidSQLiteDriver())
            .build()
        taskDao = db.taskDao()
        habitDao = db.habitDao()
        logDao = db.habitLogDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    // ---------- Helpers ----------

    private companion object {
        const val TODAY = "2026-11-10"
    }

    private suspend fun addTask(
        title: String,
        dueDate: String?,
        isCompleted: Boolean = false,
        createdAt: Long = 0L,
    ): Long = taskDao.insert(
        TaskEntity(
            title = title,
            dueDate = dueDate,
            isCompleted = isCompleted,
            createdAt = createdAt,
        )
    )

    private suspend fun addHabit(title: String): Long =
        habitDao.insert(HabitEntity(title = title, createdAt = 0L))

    // ---------- Entity mapping ----------

    @Test
    fun insertedTask_getsGeneratedId_andIsReadBackIntact() = runBlocking<Unit> {
        val id = taskDao.insert(
            TaskEntity(
                title = "Pay rent",
                description = "Transfer before noon",
                dueDate = "2026-11-12",
                reminderAt = 1_794_000_000_000L,
                createdAt = 42L,
            )
        )

        assertNotEquals(0L, id)
        assertEquals(
            TaskEntity(
                id = id,
                title = "Pay rent",
                description = "Transfer before noon",
                isCompleted = false,
                dueDate = "2026-11-12",
                reminderAt = 1_794_000_000_000L,
                createdAt = 42L,
            ),
            taskDao.getById(id),
        )
        assertNull(taskDao.getById(id + 1))
    }

    // ---------- Habit logs ----------

    @Test
    fun deletingHabit_cascadesToItsLogs_butNotToOtherHabits() = runBlocking<Unit> {
        val kept = addHabit("Read")
        val deleted = addHabit("Run")
        logDao.insert(HabitLogEntity(kept, "2026-11-01"))
        logDao.insert(HabitLogEntity(deleted, "2026-11-01"))
        logDao.insert(HabitLogEntity(deleted, "2026-11-02"))

        habitDao.deleteById(deleted)

        assertNull(habitDao.getById(deleted))
        assertEquals(
            listOf(HabitLogEntity(kept, "2026-11-01")),
            logDao.observeAll().first(),
        )
    }

    @Test
    fun duplicateLog_isIgnored() = runBlocking<Unit> {
        val habitId = addHabit("Read")
        val log = HabitLogEntity(habitId, "2026-11-01")

        val first = logDao.insert(log)
        val second = logDao.insert(log)

        assertNotEquals(-1L, first)
        assertEquals(-1L, second)
        assertEquals(listOf(log), logDao.observeAll().first())
    }

    @Test
    fun logForUnknownHabit_isRejected() = runBlocking<Unit> {
        val outcome = runCatching {
            logDao.insert(HabitLogEntity(habitId = 999L, date = "2026-11-01"))
        }

        assertTrue("Foreign key should reject a missing habit", outcome.isFailure)
        assertTrue(logDao.observeAll().first().isEmpty())
    }

    @Test
    fun observeBetween_includesBothEnds_inOrder_forThatHabitOnly() = runBlocking<Unit> {
        val reading = addHabit("Read")
        val running = addHabit("Run")
        listOf("2026-11-03", "2026-11-01", "2026-11-05", "2026-11-02").forEach {
            logDao.insert(HabitLogEntity(reading, it))
        }
        logDao.insert(HabitLogEntity(running, "2026-11-02"))

        val dates = logDao
            .observeBetween(reading, "2026-11-02", "2026-11-03")
            .first()
            .map { it.date }

        assertEquals(listOf("2026-11-02", "2026-11-03"), dates)
    }

    @Test
    fun deleteLog_removesOnlyThatDay_andIsHarmlessIfMissing() = runBlocking<Unit> {
        val habitId = addHabit("Read")
        logDao.insert(HabitLogEntity(habitId, "2026-11-01"))
        logDao.insert(HabitLogEntity(habitId, "2026-11-02"))

        logDao.delete(habitId, "2026-11-01")
        logDao.delete(habitId, "2030-01-01") // no such log: must not fail

        assertEquals(listOf("2026-11-02"), logDao.observeAll().first().map { it.date })
    }

    // ---------- Task groups ----------

    @Test
    fun observeDueFrom_returnsTodayAndLater_pendingBeforeCompletedWithinADay() = runBlocking<Unit> {
        addTask("overdue", "2026-11-09")
        addTask("today-done", TODAY, isCompleted = true, createdAt = 1)
        addTask("today-pending", TODAY, createdAt = 2)
        addTask("tomorrow", "2026-11-11")
        addTask("undated", null)

        val titles = taskDao.observeDueFrom(TODAY).first().map { it.title }

        assertEquals(listOf("today-pending", "today-done", "tomorrow"), titles)
    }

    @Test
    fun observeOverdue_returnsOnlyPendingPastTasks_oldestFirst() = runBlocking<Unit> {
        addTask("recent", "2026-11-09")
        addTask("older", "2026-11-05")
        addTask("past-but-done", "2026-11-08", isCompleted = true)
        addTask("today", TODAY)
        addTask("undated", null)

        val titles = taskDao.observeOverdue(TODAY).first().map { it.title }

        assertEquals(listOf("older", "recent"), titles)
    }

    @Test
    fun observeUndated_returnsOnlyTasksWithoutDueDate_pendingFirst() = runBlocking<Unit> {
        addTask("done", null, isCompleted = true, createdAt = 1)
        addTask("pending-1", null, createdAt = 2)
        addTask("pending-2", null, createdAt = 3)
        addTask("dated", TODAY)

        val titles = taskDao.observeUndated().first().map { it.title }

        assertEquals(listOf("pending-1", "pending-2", "done"), titles)
    }

    @Test
    fun completingTask_removesItFromOverdue() = runBlocking<Unit> {
        val id = addTask("late", "2026-11-09")
        assertEquals(1, taskDao.observeOverdue(TODAY).first().size)

        taskDao.setCompleted(id, true)

        assertTrue(taskDao.observeOverdue(TODAY).first().isEmpty())
    }
}