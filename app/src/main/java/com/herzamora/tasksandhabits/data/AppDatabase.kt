package com.herzamora.tasksandhabits.data
import android.content.Context
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver
import com.herzamora.tasksandhabits.data.dao.HabitDao
import com.herzamora.tasksandhabits.data.dao.HabitLogDao
import com.herzamora.tasksandhabits.data.dao.TaskDao
import com.herzamora.tasksandhabits.data.entity.HabitEntity
import com.herzamora.tasksandhabits.data.entity.HabitLogEntity
import com.herzamora.tasksandhabits.data.entity.TaskEntity

@Database(
    entities = [
        TaskEntity::class,
        HabitEntity::class,
        HabitLogEntity::class,
    ],
    version = 1,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun habitDao(): HabitDao
    abstract fun habitLogDao(): HabitLogDao
}

/** Name of the SQLite file, stored in the app's private storage. */
private const val DATABASE_NAME = "tasks_and_habits.db"

/** Builds the app's database. */
fun buildAppDatabase(context: Context): AppDatabase =
    Room.databaseBuilder<AppDatabase>(context.applicationContext, DATABASE_NAME)
        .setDriver(AndroidSQLiteDriver())
        .build()