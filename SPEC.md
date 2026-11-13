# MyFit — Specification

> A personal, offline Android app for building workouts, running them with a reliable timer and tracking body progress.

## Problem and why it matters

People who lift regularly need three things at once: knowing what to do (order, sets, reps, load, rest), executing without friction mid-workout (logging the set, resting for the right amount of time, adjusting the load) and seeing progress over months (frequency, loads, measurements, photos). Today this is scattered across a notes app, the phone's timer, spreadsheets and the photo gallery. The result is lost history, rest timed "by eye" and no view of progress.

Fitness apps on the market solve part of this, but they require an account, send data to third-party servers, push subscriptions and gamification, and have cluttered interfaces. For daily personal use, the cost is friction in every workout and data that doesn't really belong to the user.

MyFit is a single-user app: fast during the workout, 100% local, with encrypted backups under the user's own control.

## Goals

- Run a full workout (sets, rests, adjustments) without losing focus on the exercise, with a correct timer even with the screen locked or the app in the background.
- Never lose an in-progress session: closing the app, killing the process or restarting the phone doesn't erase workout progress.
- Keep the full history of workouts and measurements, browsable through a calendar, summaries and charts.
- Protect data against device loss with automatic, encrypted backups, without a backend of its own.
- Have its own visual identity, minimal and dark, that doesn't look like a fitness app template.

## Non-goals

- Not a social network: no sharing, rankings, friends or feed.
- No gamification: no badges, points or streaks that pressure you not to "break" them.
- No account, login or server: data lives on the device and in the user's backup files.
- Not a coach: it doesn't generate workouts, prescribe loads or give health or nutrition advice.
- No integration with wearables, Health Connect or heart-rate sensors.

## Success criteria

| Criterion | Measurement | Target |
|---|---|---|
| Rest accuracy | Deviation between the actual end of rest and the alert, with the screen locked | ≤ 2 s |
| Session recovery | Workout resumed at the same exercise/set after killing the process or restarting the phone | 100% of the test script cases |
| Execution friction | Taps to complete a set and start the rest | 1 tap |
| App launch | Time until Home is interactive (cold start, mid-range phone) | < 1.5 s |
| Backup integrity | Restoring a backup recreates an identical database and images (counts and hashes) | 100% in round-trip tests |
| Corruption detection | Tampered file or wrong password rejected before touching local data | 100% in tests |
| APK size | Minified release APK | < 15 MB |
| User acceptance criteria | Manual checklist in `docs/TESTING.md` on the user's phone | All items pass |

## User and use cases

**User:** one person (the app's owner) who lifts 3 to 6 times a week on a split routine (e.g., Back, Chest, Legs), uses a Samsung phone with a recent Android version and wants their history under their own control.

**Main use cases:**

1. "I get to the gym, open the app, tap Start on the Back workout and log sets as I go; the rest timer runs on its own and the phone vibrates/rings when it ends, even while locked on the bench."
2. "Mid-workout the machine is taken: I skip the exercise, do the next one and come back later; I bump the load from 40 to 42.5 kg on one set."
3. "The app closed (or the phone restarted) mid-workout: I reopen it and pick up where I left off, with the correct total time."
4. "At the end of the month, I check the calendar to see how many times I trained, the total time and which exercises I did."
5. "Every 4 weeks I log my weight and measurements, compare them with the initial assessment and watch the arm and waist charts go up and down."
6. "I switched phones: I install the app, point it to the backup folder synced with Drive, type the password and everything is back, photos included."

## Functional requirements

### Home (dashboard)

1. MUST show a greeting and the current date.
2. MUST show, with top priority, an in-progress workout with a "Resume" action.
3. MUST show the workout done today, if any.
4. MUST offer a shortcut to start a workout, suggesting the one done longest ago.
5. MUST list the available workouts, a summary of the latest activity, the most recent body metrics (with change) and the week summary.

### Workouts and exercises

6. MUST allow creating, editing, duplicating and deleting workouts (name, optional description, ordered list of exercises), with no limit on how many.
7. MUST allow adding, removing and reordering exercises in a workout.
8. Each exercise in a workout MUST have: sets, reps per set (or time per set for exercises measured in time, such as planks and cardio), rest between sets (s), load (kg, optional) and notes.
9. MUST offer a library of predefined exercises (name, muscle group, push/pull/legs/other movement type, a line drawing of the start and end position) with search and filtering by group.
10. MUST allow creating custom exercises with an optional demo image picked from the gallery and whether each set counts repetitions or time; a photo picked by the user takes precedence over the library drawing.
10a. SHOULD show a body map highlighting the exercise's muscle group (live while editing it, and as the picture of exercises without a photo or drawing) and offer a video search for the exercise, opened in another app.

### Workout execution

11. MUST start a session from a workout and show: workout name, current exercise, image (if any), current set/total, planned reps, load, rest timer and total time.
12. MUST follow the flow: complete set (1 tap) → automatic rest → visual and audible alert when it ends → next set → next exercise → finish.
13. MUST allow pausing/resuming, skipping a set, skipping an exercise, adjusting the set's load and reps (or time), changing the rest (including ±15 s during the rest) and ending early. Timed sets SHOULD offer a countdown that records the time actually held.
14. MUST keep timers correct with the app in the background, the screen locked, the process killed by the system or after a device restart. Time MUST be derived from persisted timestamps, never from UI counters.
15. MUST signal the end of rest with the app's own sound and vibration even with the app closed (by default at alarm volume, so it plays even with the phone on vibrate), SHOULD beep in the last 3 seconds of rest (can be turned off), and show an ongoing workout notification with the timer and quick actions.
16. MUST persist every session step immediately and offer exact resumption when the app is reopened.
17. When finishing, MUST automatically save: date, start and end time, duration (excluding pauses), workout, exercises, completed/skipped sets, and the reps and loads performed.
18. SHOULD offer, when finishing, to update the workout's loads with the ones used in the session.
18a. SHOULD let the user rate the workout's intensity (low, medium, high) and attach a photo (stored reduced) when finishing, both optional and editable later in History.

### History and calendar

19. MUST show a monthly calendar with workout days highlighted.
19a. MUST allow logging other sports after the fact (swimming, cycling, indoor cycling, running, treadmill, walking, soccer, volleyball, basketball, beach tennis, muay thai, jiu-jitsu, boxing, yoga, pilates, stretching, or a free activity named by the user), each with a figure, date, duration, optional distance, intensity, notes and photo; they appear in the calendar, the day list, the summaries and the Home week. SHOULD also allow timing an activity live (pause/resume, surviving a locked screen or a killed app) and, for outdoor sports and with permission, measuring distance with the phone's GPS without internet and without storing the route.
20. When a day is tapped, MUST list its sessions with duration and allow opening the detail (exercises, sets, reps, loads).
21. MUST show weekly and monthly summaries: number of workouts and activities, total time, average weekly frequency, exercises performed and sports practiced.
22. History MUST stay intact even if the source workout or exercise is later edited or deleted.

### Body progress

23. MUST allow creating, editing and deleting assessments with date, weight (kg), body fat % (optional) and 14 circumferences in cm (neck, shoulders, chest, waist, abdomen, hips, right/left arms, right/left forearms, right/left thighs, right/left calves), all optional except the date.
24. MUST list the assessment history.
25. MUST compare two chosen assessments, showing values and the signed absolute difference per metric.
26. MUST show progress charts over time for each metric, with selection of the metrics displayed (including all at once).
27. MUST allow logging body photos by date (front/side/back), including past dates for older photos, browsing by date and comparing two photos side by side.

### Backup and data

28. MUST generate a single, compressed backup with all data (workouts, custom library, full sessions, measurements, photos, exercise images, settings), including the format version and metadata.
29. MUST encrypt every backup with a user-defined recovery password.
30. MUST write backups to a user-chosen folder on the device (which they can sync with Google Drive on their own), keeping the 7 most recent.
31. MUST back up automatically every day and after important changes, with a minimum interval to avoid excessive writes, without getting in the way of offline use.
32. MUST allow manual backups and clearly show the date/time of the last backup, the status and the last error.
33. MUST allow restoring a backup from the folder or from a standalone file, showing the backup date and asking for confirmation before replacing local data.
34. MUST validate integrity (password, authenticity, hashes, format version) before restoring and again afterwards, without changing local data if validation fails.

### Settings

35. MUST allow choosing the theme (Dark, default; Light; System), the default rest, the rest sound behavior and keeping the screen on during workouts.
35a. SHOULD allow scheduling workout reminders (a workout or activity, a time, and either a date or weekly days) that notify at that time.
36. MUST bring together backup management (folder, password, automatic, manual, restore) and exercise library management.
37. SHOULD guide the user to remove battery optimization for the app (relevant on Samsung devices).

## Non-functional requirements

- **Offline-first:** every feature works without internet; the app makes no network calls (the video search is handed to another app).
- **Third-party content:** bundled illustrations keep their CC BY-SA license, with author, source, license and changes listed in the app.
- **Reliability:** data preserved after a device restart; database with migrations tested from the first installed version; atomic (all-or-nothing) restore.
- **Privacy and security:** no data collection, no analytics, no media permissions (system photo picker); backups encrypted with authentication (streaming AES-256-GCM, password-derived key); Android auto backup disabled so it doesn't copy keys that don't work on another device.
- **Performance:** 60 fps UI during workout execution; smooth history queries and charts with 5 years of data (~1,500 sessions, ~60 assessments).
- **Experience:** dark theme first and a complete light theme; layouts adapted to phones of different sizes and to large screens; subtle animations (150–250 ms); large touch targets during execution; support for large system font sizes.
- **Compatibility:** Android 8.0 (API 26) or higher; target API 37.
- **Language:** UI in English by default with a Brazilian Portuguese translation (strings in `res/values` and `res/values-pt-rBR`); metric units (kg, cm); locale-aware number and date formatting.

## Visual identity

- MyFit brand with its own symbol and logotype, adaptive icon and monochrome version.
- Mostly black, white and grays; green as the only accent color, reserved for progress and positive actions.
- Aesthetic references: Linear, Apple Fitness, Whoop. Avoid looking like a fitness app template.
- Consistent component system across all screens, documented in `docs/BRAND.md`.

## Architecture overview

Native Android app (Kotlin, Jetpack Compose, Material 3), MVVM with UI, domain and data layers:

```
ui/       Compose screens + ViewModels per feature; shared design system
domain/   pure models and rules (workout session engine, summaries, comparisons, backup format)
data/     Room (source of truth), DataStore (settings), image storage, backup folder (SAF)
```

Platform components: a foreground service for the workout session, an exact alarm for the end of rest, WorkManager for backups. Details in `docs/ARCHITECTURE.md`.

## Constraints and assumptions

- Distributed as a signed APK installed manually (no Play Store). Assumption: the user can install apps from outside the store on their device, either through the unverified developer flow or via ADB.
- No backend, authentication or paid services.
- Syncing the backup folder to the cloud is the user's responsibility (with a sync app of their choice). Assumption: the folder's document provider allows creating, listing and deleting files.
- Developed and validated without an Android device: JVM tests (unit, Robolectric, screenshots). Platform behavior (alarms, service, sound, real folder) is validated by the user with a manual checklist.
- Losing both the device and the recovery password makes the backups unrecoverable, by design.

## Out of scope (v1)

- Real-time sync between devices.
- Direct integration with the Google Drive API (replaced by the user-chosen folder).
- Export to CSV/spreadsheet.
- Supersets, drop sets and sets with different targets per set in planning (what was actually done is logged per set).
- Automatic calculation of 1RM, volume per muscle group and personal records.
- Home screen widgets, Wear OS and Android 16 Live Updates.
- Other languages and imperial units.

## Open questions

1. **Installing outside the Play Store in Brazil:** the verified developer requirement was scheduled for September 30, 2026. Before the release, confirm whether it is in effect and document the path (advanced flow, ADB or limited distribution account).
2. **Document providers:** sync apps expose folders with different behaviors (renaming, listing). The fallback is to write directly to the final name; validate on the user's device.
3. **Updating workout loads when finishing:** checked by default (option visible on the summary screen). Revisit after a few weeks of use.
4. **Next workout suggestion:** "the one done longest ago" is the initial rule; it may become a fixed rotation order if it doesn't reflect the user's split.
