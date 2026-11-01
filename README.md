# Tasks & Habits

A native Android app for personal daily productivity: simple, date-bound **tasks** and recurring **daily habits** in one lightweight, offline-first experience.

> **Status:** work in progress. Phase 0 (setup) is done and Phase 1 (data layer) is next. There is no usable app yet.

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
| Local database | Room 3 (`androidx.room3`) with KSP |
| Async & state | Coroutines, Kotlin Flow, ViewModel (`StateFlow`) |
| Navigation | Navigation Compose |
| Reminders | `AlarmManager` (exact alarms) + `BroadcastReceiver`s + notifications |
| Dependency injection | Manual (a simple container), no framework yet |
| Min SDK | 26 (Android 8.0) |
| Build | Gradle with Kotlin DSL, version catalog |
| Tests | JUnit for logic, instrumented tests for the database |

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

## Roadmap

- [x] **Phase 0: Setup.** Project from the template, running on a physical device, repository and README on GitHub.
- [ ] **Phase 1: Data layer.** Room 3 + KSP, entities, DAOs, in-memory database tests.
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

### Still open

Proposed defaults are used until decided.

| Question | Proposed default |
| :--- | :--- |
| What if a reminder is saved with a time already in the past? | Reject it with a validation message. |
| Can a task have a reminder without a due date? | Yes, the two are independent. |
| Where do overdue pending tasks appear? | At the top of "Today", visually marked. |
| Does the MVP offer both archive and delete for habits? | Both: archive hides a habit but keeps its history; delete removes it with its logs. |

## Data model (planned)

* **Task:** title, optional description, completed flag, optional due date (date only), optional reminder (exact instant).
* **Habit:** title, archived flag, creation time.
* **HabitLog:** one row per habit per day, with a composite key `(habitId, date)`. A row means the habit was done that day.

The full schema will be documented here once it is implemented in Phase 1.

## Getting started

**Requirements:** Android Studio Quail 3 or newer, and a device or emulator running Android 8.0 (API 26) or higher.

```bash
git clone https://github.com/herzamora/TasksAndHabits.git
```

Open the folder in Android Studio, wait for the Gradle sync, and run the `app` configuration. From the command line, with a device connected:

```bash
./gradlew installDebug
```

## Learning log

### Phase 0: Setup

* Development environment on Linux: KVM acceleration configured for the emulator, 32-bit compatibility libraries installed (`libc6:i386`, `libncurses6:i386`, ...), and Android Studio installed through JetBrains Toolbox.
* Project created from the **Empty Activity** (Compose) template: package `com.herzamora.tasksandhabits`, min SDK 26, Gradle Kotlin DSL.
* Enabled USB debugging, connected the phone through ADB, and deployed the template ("Hello Android!") to a physical device.
* Initialized the repository with a first commit from the untouched template and pushed `main` to GitHub.
