# Tasks & Habits

A native Android app for personal daily productivity: simple, date-bound **tasks** and recurring **daily habits** in one lightweight, offline-first experience.

> **Status:** work in progress. Phase 0 (setup) and Phase 1 (data layer) are done; Phase 2 (domain & ViewModels) is next. The database layer is implemented and tested, but there is no usable UI yet.

This is my **first Android project**. The goal is to learn modern Android development and to document the process in the open: the decisions, the trade-offs, and what I learn along the way.

## Planned features (MVP)

* **Tasks:** create, edit, and delete tasks; undated tasks and tasks with a due date; mark as completed; list grouped into "Today / Upcoming" and "No date".
* **Reminders:** exact date and time, local notification, handling of the notification and exact-alarm permissions, and re-scheduling after a device reboot.
* **Habits:** create and archive/delete habits, mark today as done, per-day status (done / missed), and a current streak counter.
* **Privacy:** everything stays on the device. No accounts, no server, no network.

**Not in the MVP:** subtasks, tags and categories, attachments, location-based or custom-repeat reminders, weekly/monthly habit schedules, charts and data export, habit reminders.

## Tech stack

| Area | Choice |
| :--- | :--- |
| Language | Kotlin 2.x |
| UI | Jetpack Compose + Material Design 3 |
| Architecture | MVVM with unidirectional data flow |
| Local database | Room 3 (`androidx.room3`) with KSP and the Room Gradle plugin (schema export) |
| SQLite driver | `AndroidSQLiteDriver` (the SQLite that ships with Android) |
| Async & state | Coroutines, Kotlin Flow, ViewModel (`StateFlow`) |
| Navigation | Navigation Compose |
| Reminders | `AlarmManager` (exact alarms) + `BroadcastReceiver`s + notifications |
| Dependency injection | Manual (a simple container), no framework yet |
| Min SDK | 26 (Android 8.0) |
| Build | Gradle with Kotlin DSL, version catalog |
| Tests | JUnit for logic, instrumented tests (in-memory Room database) for the data layer |

## Architecture

```
 UI (Compose screens)
   │  state ↓   events ↑
 ViewModel (StateFlow)
   │
 Repository ─────────────► ReminderScheduler (AlarmManager)
   │
 DAO ◄─► Room database
```

Pure logic (habit status, streaks) lives in plain Kotlin, separate from Android classes, so it can be unit-tested.

The data layer (entities, DAOs, database) is implemented and tested. The layers above it are next.

## Project structure

```
app/src/main/java/com/herzamora/tasksandhabits/
└── data/
    ├── AppDatabase.kt        database class + builder (driver configuration)
    ├── dao/                  TaskDao, HabitDao, HabitLogDao
    └── entity/               TaskEntity, HabitEntity, HabitLogEntity

app/src/androidTest/java/com/herzamora/tasksandhabits/
└── data/
    └── DaoTest.kt            instrumented tests on an in-memory database

app/schemas/                  exported Room schema (version-controlled)
```

## Roadmap

- [x] **Phase 0: Setup.** Project from the template, running on a physical device, repository and README on GitHub.
- [x] **Phase 1: Data layer.** Room 3 + KSP, entities, DAOs, database class, in-memory database tests.
- [ ] **Phase 2: Domain & ViewModels.** Repositories, streak and status logic (unit-tested), UI state.
- [ ] **Phase 3: User interface.** Compose screens, navigation, date and time pickers.
- [ ] **Phase 4: Reminders.** Notification channel, permission flows, exact alarms with fallback, re-scheduling after reboot.
- [ ] **Phase 5: Testing, polish & first release.** Manual test matrix (screen off, Doze, reboot, denied permissions), UI polish, debug APK in GitHub Releases.

Phases are an order of work, not a schedule.

## Design decisions

### Decided

| Decision | Why |
| :--- | :--- |
| **Habit state is derived from logs; no daily reset job.** | A log row means "done"; "missed" and streaks are computed when needed. There is nothing to reset at midnight and no failure if the phone is off at that moment. The alternative, a WorkManager job creating rows every night, is not exact (15-minute minimum interval, subject to Doze). |
| **Room 3 instead of Room 2.x.** | It is the current stable line; Room 2.x is in maintenance mode. For a new project there is no migration cost. |
| **Date-only strings (`YYYY-MM-DD`) for calendar dates; epoch millis only for exact instants.** | "Due Friday" is a date, not a moment, so this avoids time zone problems and keeps dates sortable as text. |
| **`SCHEDULE_EXACT_ALARM` with a fallback to inexact alarms.** | Reminders are user-set and time-sensitive. From Android 14 the permission is not granted by default, so the app checks it, sends the user to settings, and degrades gracefully if it is denied. `USE_EXACT_ALARM` is meant for calendar and alarm-clock apps. |
| **Manual dependency injection.** | Keeps the wiring visible while learning. Hilt or Koin may come after the MVP as a deliberate exercise. |
| **Min SDK 26.** | `java.time` and notification channels work without compatibility layers. |
| **`AndroidSQLiteDriver` instead of `BundledSQLiteDriver`.** | It is what the official Android-only Room guide uses, and it adds no dependency or APK weight. The queries need no newer SQLite features, Android Studio's Database Inspector is built for the system SQLite, and the tests use the same driver as the app. Switching later is a one-line change. |
| **`AUTOINCREMENT` ids (Room's default).** | Ids are never reused, so an alarm tied to a deleted task's id can never fire for a different task. The Room docs advise avoiding it unless needed; at this scale the performance cost is irrelevant, so it is cheap insurance. |
| **"Today" is passed to queries as a parameter, not computed with `date('now')` in SQL.** | SQLite's `date('now')` is UTC, which can differ from the local date near midnight. Passing it in keeps the clock under the app's control and makes tests deterministic. |
| **Marking a habit is idempotent.** | Inserting a log ignores duplicates and un-marking is a plain delete, so repeating either is safe and no "does it exist?" check is needed. A log for a habit that does not exist is still rejected by the foreign key. |
| **The Habits screen observes all logs in a single `Flow` and groups them in Kotlin.** | It avoids one stream per habit. At roughly 10 habits x 365 days a year the table stays tiny. If it ever grows, windowed queries are the next step. |

### Still open

Proposed defaults are used until decided. The data layer already supports the defaults for the last three questions; the first one is a validation rule for the UI and reminder phases.

| Question | Proposed default |
| :--- | :--- |
| What if a reminder is saved with a time already in the past? | Reject it with a validation message. |
| Can a task have a reminder without a due date? | Yes, the two are independent. |
| Where do overdue pending tasks appear? | At the top of "Today", visually marked. |
| Does the MVP offer both archive and delete for habits? | Both: archive hides a habit but keeps its history; delete removes it with its logs. |

## Data model

Room schema, **version 1** (exported to `app/schemas/`).

```
+----------------------------------------------+
|                    tasks                     |
+----------------------------------------------+
| id           Long     PK, autoincrement      |
| title        String                          |
| description  String?                         |
| isCompleted  Boolean                         |
| dueDate      String?  "YYYY-MM-DD", date only|
| reminderAt   Long?    epoch millis (UTC)     |
| createdAt    Long     epoch millis (UTC)     |
+----------------------------------------------+

+----------------------------------------------+
|                   habits                     |
+----------------------------------------------+
| id           Long     PK, autoincrement      |
| title        String                          |
| isArchived   Boolean                         |
| createdAt    Long     epoch millis (UTC)     |
+----------------------------------------------+
                      │ 1
                      │
                      │ *
+----------------------------------------------+
|                 habit_logs                   |
+----------------------------------------------+
| habitId      Long     FK -> habits.id        |
|                       ON DELETE CASCADE      |
| date         String   "YYYY-MM-DD", local    |
| PK: composite (habitId, date)                |
+----------------------------------------------+
  A row exists  = the habit was done that day.
  No row        = not done.
```

* `dueDate` is a calendar date; `reminderAt` is an exact instant. They are independent.
* Deleting a habit deletes its logs. Archiving a habit (`isArchived`) hides it but keeps its history.
* There is no separate index on `habit_logs.habitId`: it is the first column of the composite primary key.

### Data access (DAOs)

| DAO | Writes (`suspend`) | Reads |
| :--- | :--- | :--- |
| `TaskDao` | `insert`, `update`, `setCompleted`, `deleteById` | `getById`; `Flow`: `observeDueFrom(today)`, `observeOverdue(today)`, `observeUndated()` |
| `HabitDao` | `insert`, `update`, `setArchived`, `deleteById` | `getById`; `Flow`: `observeActive()`, `observeArchived()` |
| `HabitLogDao` | `insert` (ignores duplicates), `delete(habitId, date)` | `Flow`: `observeAll()`, `observeBetween(habitId, startDate, endDate)` |

### Derived state (implemented in Phase 2)

* **Habit status for a date:** done if a log exists; missed if the date is before today, not before the habit was created, and has no log; otherwise pending (today) or not applicable.
* **Current streak:** count consecutive logged days going back from today. If today is not logged yet, start from yesterday, so the streak is not lost just because the day is not over.

## Getting started

**Requirements:** Android Studio Quail 3 or newer, and a device or emulator running Android 8.0 (API 26) or higher.

```bash
git clone https://github.com/herzamora/TasksAndHabits.git
```

Open the folder in Android Studio, wait for the Gradle sync, and run the `app` configuration. From the command line, with a device connected:

```bash
./gradlew installDebug
```

To run the instrumented database tests (a connected device or running emulator is required):

```bash
./gradlew connectedDebugAndroidTest
```

You can also run them from Android Studio with the green arrow next to `DaoTest`.

## Learning log

### Phase 0: Setup

* Development environment on Linux: KVM acceleration configured for the emulator, 32-bit compatibility libraries installed (`libc6:i386`, `libncurses6:i386`, ...), and Android Studio installed through JetBrains Toolbox.
* Project created from the **Empty Activity** (Compose) template: package `com.herzamora.tasksandhabits`, min SDK 26, Gradle Kotlin DSL.
* Enabled USB debugging, connected the phone through ADB, and deployed the template ("Hello Android!") to a physical device.
* Initialized the repository with a first commit from the untouched template and pushed `main` to GitHub.

### Phase 1: Data layer

* Added Room 3 and KSP through the version catalog, plus the Room Gradle plugin so the schema is exported to `app/schemas/` and version-controlled, ready for future migrations. Used the latest stable Room release (3.0.x) rather than the alpha line.
* Wrote the three entities: a composite primary key `(habitId, date)` and an explicit foreign key with `ON DELETE CASCADE` on the log table, and calendar dates stored as ISO text.
* Wrote the DAOs with `suspend` functions for writes and `Flow` for reads. In Room 3, `Flow` functions are not `suspend`.
* Defined `AppDatabase` and configured `AndroidSQLiteDriver`: Room 3 requires choosing a SQLite driver explicitly.
* Wrote 10 instrumented tests against an in-memory database and ran them on a physical phone: entity mapping, cascade delete, idempotent log insert, rejection of logs for unknown habits, the date-range query, and the three task-group queries.
* Checked that the tests can fail: removed `ON DELETE CASCADE` from the log entity, watched the cascade test fail, then restored it. The passing run also showed that SQLite foreign keys are enforced in this setup.
* Differences from most online tutorials (written for Room 2.x): the `androidx.room3` package, the mandatory SQLite driver, and `Flow` queries that are not `suspend`.
