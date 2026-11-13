# MyFit — Architecture

## Overview

Native Android app, single module (`:app`), MVVM, offline. Room is the source of truth; nothing leaves the device except the backup files the user chooses to write to a folder.

```
ui/        Compose: screens + ViewModels (Hilt), design system, navigation
  │  observes StateFlow, calls actions
domain/    pure Kotlin: session engine, history/measurement calculations, backup format
  ▲  used by
data/      Room, DataStore, ImageStore, repositories, BackupManager, SAF destination
platform/  foreground service, alarm, receivers, notifications, WorkManager
```

Dependency rule: `domain` knows nothing about Android, except for the Tink library. `ui` doesn't talk to Room directly. `platform` orchestrates `data` and `domain`.

| Package | Contents |
|---|---|
| `core/time` | `Mark` (wall clock + monotonic clock + boot counter), `TimeSource` |
| `core/format` | locale-aware numbers (`DecimalInput`), durations, dates |
| `domain/session` | `SessionEngine`, `ActiveSession` |
| `domain/history` | `HistoryCalculator` (summaries with workouts and activities, Mon–Sun weeks) |
| `domain/activity` | `Sport`, `Activity` (other sports logged after the fact) |
| `domain/body` | `Measure`, `BodyComparison` (differences, series, snapshot) |
| `domain/backup` | v1 DTOs, header and encryption, zip + manifest, retention |
| `data/db` | entities, DAOs, `MyFitDatabase` (v4), `Migrations`, library seed |
| `data/repository` | workouts, exercises, session, history, activities, body |
| `data/backup` | `BackupManager`, `BackupKeyStore`, destinations (SAF/file), `FolderBackupWriter` |
| `platform/session` | `SessionController`, `WorkoutService`, alarm, receivers, notifications |
| `platform/backup` | `BackupScheduler`, workers, failure notification, image cleanup |
| `platform/reminder` | reminder alarms, notification, re-arming on start/boot/time change |
| `platform/activity` | live activity: `LiveActivityController`, `ActivityTrackingService` (chronometer, GPS) |

## Data

```
exercise ─┐                       workout_session ── session_exercise ── session_set
          └── routine_exercise ── routine        (snapshots: name, group, image, planned/actual)
body_assessment   body_photo   activity
```

- `routine_exercise.exerciseId` uses RESTRICT. An exercise in use is archived instead of deleted.
- `workout_session.routineId` uses SET NULL, and the session tables keep copies (workout name, exercise name/group/image, targets). Editing or deleting a workout never changes history.
- `startDateEpochDay` is the local day the session started: a workout that crosses midnight counts on its start day.
- Images live in `filesDir/images/{exercises,body,sessions}/<uuid>.jpg`, stripped of EXIF: exercise and body photos downscaled to 1600 px (JPEG 85), workout and activity photos to 1280 px (JPEG 80). The database stores only the relative path. Paths that would escape `images/` don't resolve to any file.
- Schema exported to `app/schemas`. Any change requires a tested migration, because an installed APK already exists.
- Database version 2 changed no tables: migration 1 → 2 (`ExerciseSeed.MIGRATION_1_2`) only adds the exercises introduced in library version 2, skipping names that already exist in either language, so user-created and deleted exercises are respected.
- Database version 3 adds `seedKey` to `exercise` (unique) and `session_exercise` (snapshot). Migration 2 → 3 links existing library rows by name (either language; a row the user renamed stays unlinked), copies the key into session snapshots and adds the version-3 exercises. Keys are frozen in `app/src/test/resources/seed-keys.txt`.
- Database version 4 adds `intensity` and `imagePath` to `workout_session` and the `activity` table (sport, optional name for "free", date, minutes, distance in km, intensity, notes, photo). Migration 3 → 4 (`Migrations.MIGRATION_3_4`) only adds them; existing sessions get no intensity.
- Database version 5 adds `measure` (REPS or TIME) to `exercise` and to the `session_exercise` snapshot. For timed exercises the `reps` columns hold seconds (targets and what was done), so sessions, the engine and backups need no other change. Migration 4 → 5 makes the library planks and cardio timed and gives their workout targets below 15 a time default (30 s, or 10 min for cardio); past sessions keep what was recorded.
- `MigrationTest` builds real version-1, 2 and 3 databases from `app/schemas/.../<n>.json` and opens them with the current app (`Migrations.ALL`).

## Other sports

- `Activity` is logged after the fact (no live timer): it has no sets, so it lives in its own table and joins workouts only in History and the Home week (`HistoryCalculator.activeDays`, `summarize(..., activities)`).
- Figures: every `Sport` has a Material icon; indoor cycling and treadmill reuse the Workout Guide drawings (`wg-cycling`, `wg-running`).
- Distance is stored in km; swimming is typed and shown in meters.
- **Live timing:** `LiveActivity` (marks, pauses, GPS distance) is kept in memory and in its own DataStore (`live_activity`, not backed up), written on every action and at most every 10 s for GPS updates. `ActivityTrackingService` (foreground, types `health` and `location`) shows the chronometer and pause/resume and, for outdoor sports with location permission, asks `LocationManager` for GPS fixes every 2 s. `GpsDistance` drops fixes worse than 30 m, steps under 4 m and jumps faster than the sport allows; pausing clears the anchor so a paused stretch never counts. *Finish* pauses the timer and opens the "activity done" step (`ActivityEditorRoute(review = true)`); only its *Save* calls `finish(intensity, notes, photo)`, which saves the `Activity` (minutes rounded up, km) and stops timing. Only the distance is kept, never the route.

## Reminders

- Stored in DataStore as part of `AppSettings.reminders` (one JSON key), so the backup's `settings.json` carries them; no table.
- `ReminderSchedule.nextTrigger` (pure) returns the next instant strictly after now: weekly on the chosen days, or once on a date. DST gaps move the time forward.
- `ReminderScheduler` arms one `AlarmManager` alarm per enabled reminder (exact when allowed). `ReminderSync`, started from `MyFitApp`, re-arms everything whenever the list changes (edits and backup restores); a manifest receiver does it after boot, time or time zone changes and app updates.
- When it fires, the notification goes out on the "Reminders" channel; a weekly reminder arms its next occurrence and a one-off turns itself off.

## Exercise pictures

- **Drawings:** every library entry names a line drawing (`ExerciseSeed.art`). The files are alpha masks in `app/src/main/assets/exercises/<art>-1.webp` (start) and `-2.webp` (end, absent for static holds), tinted with the theme colour at draw time. `tools/exercise_art.py` (`uv run tools/exercise_art.py`) rebuilds them from pinned commits of Everkinetic and Workout Guide (both CC BY-SA 4.0); attribution is in the folder's `NOTICE.md` and in *Settings → Image credits*.
- **Link:** the drawing is found through `exercise.seedKey`, never the name, so renaming a library exercise keeps it. Session snapshots carry the key too.
- **Precedence** (`ExerciseVisual`, `ExerciseThumbnail`): the user's photo, then the drawing, then the muscle map.
- **Muscle map** (`MuscleMap`): one fixed vector body, front and back, drawn in Compose from path data; the regions of the selected group use the accent colour. Full body lights everything; cardio and "other" light nothing.
- **Video:** *Watch how to do it* opens a YouTube search for the exercise name with `ACTION_VIEW`; MyFit has no internet permission.

## Workout session

**Pure engine** (`SessionEngine`): each action (`completeSet`, `skipSet`, `skipExercise`, `goToExercise`, `adjustRest`, `pause`, `resume`…) returns a new `ActiveSession`. Main rules:

- The cursor moves to the exercise's next pending set, then to the following exercises and, finally, back to earlier ones that were left pending.
- Rest starts after completing a set, except on the workout's last set. Skipping doesn't trigger a rest.
- The load used in a set carries over to the pending sets of the same exercise.
- Pausing freezes the total time and the rest. On resume, the rest restarts with the time that was left.

**Time** (`Mark`): each mark stores `wallMs`, `elapsedMs` and `bootCount`. Within the same boot the monotonic clock is used, which is immune to time changes; after a reboot the wall clock is used. The result is never negative. The UI only redraws from the marks: a 250 ms ticker in the ViewModel runs only while the screen is collecting state.

**Persistence**: `SessionController` serializes actions with a mutex. Each action runs the engine, writes the whole session in one transaction, reschedules or cancels the alarm and updates the notifications.

**Background**:
- **Ongoing notification**: a foreground `WorkoutService` of type `health` (the API 34+ requirement is met by `HIGH_SAMPLING_RATE_SENSORS`, a normal permission) shows the system chronometer counting down during rest and counting up for the rest of the workout, with the "Complete set" and "Skip rest" actions. If the user swipes the notification away, it comes back.
- **Rest end alarm**: uses `AlarmManager.setAlarmClock`, the only exact alarm immune to Doze and One UI optimizations, plus a second one 3 s earlier for the countdown. `RestEndHandler` ignores stale alarms; the countdown alarm beeps at 3, 2 and 1 s left (re-reading the session each second), and the end alarm posts a silent heads-up notification with vibration and plays MyFit's own "go" sound.
- **Sounds** (`RestSoundPlayer`, SoundPool): `res/raw/rest_tick.wav` (countdown beep) and `rest_go.wav` (end), chosen by the user and converted to 44.1 kHz mono WAV, trimmed and peak-normalized to about −2 dB. They play on the alarm stream by default (heard on vibrate or silent) or on the notification stream with *Follow phone mode*. The notification channel is `rest_end_v2`; the 1.4 channels, which played the phone's sound, are deleted.
- **Recovery**: if the process dies, the session remains in the database. When the app returns to the foreground, it calls `resync()`, which re-arms the alarm and the service, and shows the "Resume" bar.

## Timed sets

- `TimedSets` sets the defaults and the ± steps (5 s under a minute, 15 s under five, 1 min above); `Durations.setTime` formats "45 s", "1:30 min", "10 min".
- In the workout, *Start* holds a timed set: `WorkoutViewModel` keeps the start mark in memory (not persisted: if the app dies mid-hold the set just isn't done), plays the countdown beeps and the "go" sound through `RestSoundPlayer`, keeps counting past the target, and *Done* completes the set with the seconds actually held. The summary's volume leaves timed exercises out.

## Backup

**File** (`.myfit`):

```
"MYFITBAK" | u16 version | u8 kdf | u32 iterations | 16 B salt | 16 B verifier | u64 createdAt
streaming AES-256-GCM-HKDF (Tink, 1 MiB segments, entire header as associated data)
  └─ zip: manifest.json (formatVersion, app, counts, SHA-256 per entry)
          data.json (BackupDataV1, independent of the Room schema)
          settings.json
          images/<type>/<uuid>.jpg (no recompression)
```

**Key**:
- **Derivation**: the key comes from PBKDF2-HMAC-SHA256 (600k iterations, password normalized to NFKC) applied to the recovery password. The verifier is an HKDF of the key, not the key itself, and makes it possible to tell "wrong password" apart from "corrupted file".
- **Storage**: the derived key is stored wrapped by an Android Keystore AES key that doesn't require unlocking, so the worker can run with the screen locked. If that key disappears (reinstall or cleared data), the state becomes LOCKED and the app asks for the password again.
- **Third-party files**: weak parameters from an imported file are never adopted.

Exercise and session DTOs carry an optional `seedKey`; restoring a backup made before version 3 links library rows by name inside the same transaction. Session DTOs carry optional `intensity` and `imagePath`, and `activities` is a new optional list. Its count goes into the manifest only when there are activities, so backups made by earlier versions still pass the exact count check.

**Restore** (`BackupManager.restore`):
1. Refuses if a workout is in progress. Reads the header, gets the key (the device's, or the one derived from the typed password) and checks the verifier.
2. Decrypts everything to the cache, because truncation and tampering only show up at the end of the stream. Validates names (no zip-slip), limits, format version, hashes, counts and image paths.
3. Copies the new images to `images/` before the commit. Names are UUIDs, so the old ones stay in place.
4. Replaces the database in a single transaction, imports the settings and only then deletes the images that no row references.
5. Revalidates counts and images.

If the app dies at any point, the database stays whole (either the old one or the new one) along with all of its images. On startup, `recoverInterruptedRestore` cleans up the leftovers.

**Folder and scheduling**:
- **Destination**: `SafBackupDestination` writes, via SAF, to the chosen folder (persistable permission). It writes `<name>.partial`, renames it and rereads the header; only then does it delete the extra files, keeping 7 and never the file it just wrote. Files that don't belong to MyFit are ignored.
- **Sync**: left to a Drive sync app. MyFit doesn't talk to the network.
- **When it runs**: `BackupScheduler` schedules a daily run (battery and storage not low), a 10 min debounce after important changes, and "Back up now".
- **Change counter**: `changeCount` goes up with every change and `savedChangeCount` records what the last backup covered. A change made during a backup stays pending.
- **Failures**: become state (folder unavailable, no password, no space, unknown) and a notification.

## Navigation and UI

- Navigation Compose with type-safe routes, in a `NavigationSuiteScaffold`: bottom bar on phones and rail on large screens, hidden on full-screen screens. Content is capped at 640 dp.
- Workout execution is immersive, always in the dark theme, with the screen kept on.
- The theme comes from the settings (dark by default). Color tokens, typography (Geist with tabular figures) and components are in `docs/BRAND.md`.

## Tests

| Layer | Tool | Examples |
|---|---|---|
| Pure domain | JUnit | session engine (17 cases), `Mark`, summaries, body comparison, crypto, zip, retention |
| Data | Robolectric + in-memory Room | DAOs, cascades and constraints, repositories, snapshot round trip, backup create/restore/corrupt |
| Platform | Robolectric shadows | alarm scheduled/canceled/rescheduled, notifications, service, workers (`work-testing`) |
| UI | Compose test + Robolectric | full workout flow, navigation, session recovery |
| Visual | Roborazzi | screenshots of every screen in dark and light themes, 1.5× font and tablet |

```bash
./gradlew testDebugUnitTest          # all tests
./gradlew recordRoborazziDebug       # generates PNGs in app/build/outputs/roborazzi/
./gradlew lintDebug
```

## Decisions and trade-offs

- **Folder instead of the Drive API**: user request. It removes the need for OAuth and Google Cloud. Sync is left to another app, and the official Drive picker doesn't grant access to a whole folder.
- **`setAlarmClock`**: reliability above all for the rest timer. The side effect is the alarm icon in the status bar while the rest is running.
- **Single module**: the app is personal and small, so the boundaries live in the packages and the dependency rule.
- **Navigation 2 and Room 2.8**: stable and well documented. Navigation 3 and Room 3 are recent and wouldn't bring any gain here.
- **No device during development**: platform validation is done with Robolectric shadows plus the manual checklist in `docs/TESTING.md`.
