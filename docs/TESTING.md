# MyFit — Testing

## Automated

```bash
export JAVA_HOME=~/.local/share/jdks/jdk-21.0.13+9 ANDROID_HOME=~/Android/Sdk
./gradlew testDebugUnitTest      # unit + Robolectric (domain, database, platform, UI)
./gradlew recordRoborazziDebug   # screenshots in app/build/outputs/roborazzi/
./gradlew lintDebug
./gradlew assembleRelease        # R8 build (signed if MYFIT_* is configured)
```

What each layer covers is described in [ARCHITECTURE.md](ARCHITECTURE.md#tests).

## Manual checklist on the phone

Development was done without a device. The items below validate what only the device can prove: sound, locked screen, service, real folder and One UI behavior. Check off each item; if something fails, note the step and the phone model.

UI labels are quoted in English; on a phone set to Portuguese, the same labels appear in Portuguese.

### 0. Installation and permissions
- [ ] Install the APK (`adb install -r myfit-<version>.apk` or the advanced flow, see README).
- [ ] Open it: dark theme, five tabs in the bottom bar.
- [ ] *Settings → Battery → Allow*: set MyFit to **Unrestricted**. The status then shows ✓.

### 1. Workouts
- [ ] Create "Back Workout" with 3 exercises from the library (searching "remada" without accents works).
- [ ] Edit sets, reps, rest and load, typing "42,5" (comma). Reorder by dragging the handle.
- [ ] Create a custom exercise with an image from the gallery. It shows up in the list and in the workout.
- [ ] Duplicate the workout ("(copy)" appears) and delete the copy.
- [ ] The library shows a drawing next to every predefined exercise. Open one: start → end drawing, the body map with its group in green, and "Watch how to do it" opens a YouTube search.
- [ ] In the exercise editor, change the muscle group: the body map updates at once. Pick a photo: it replaces the drawing; remove it: the drawing comes back.
- [ ] A custom exercise without a photo shows the body map in the list and during the workout.
- [ ] *Settings → Image credits* lists the illustrations and their licenses; *About* shows "Developed by luannamorim".

### 2. Execution (with the app open)
- [ ] Start: the first time, the app asks for notification permission. Accept it.
- [ ] The set screen shows the exercise drawing (or the body map); the play button opens the video search.
- [ ] Complete set → the rest starts on its own. Use +15 s and −15 s.
- [ ] At the end of the rest: vibration, sound and a green pulse; "REST COMPLETE" appears.
- [ ] Adjust the load with +/− and complete the set: the next set inherits the load.
- [ ] Pause (the total time stops) and resume. Skip set and skip exercise; tap the skipped exercise in the top strip to go back to it.

### 3. Locked screen and background
- [ ] Complete a set and **lock the screen** during the rest: at the end, the phone rings and vibrates, and the notification shows "Rest complete".
- [ ] On the lock screen, the notification shows the timer and the "Skip rest" button works.
- [ ] With the phone on **vibrate** and *Settings → Rest end sound → Always ring* (the default): the alert rings anyway, at the alarm volume.
- [ ] The last 3 seconds of rest beep (also with the screen locked), then MyFit's own two-note sound plays, not the phone's notification sound. *Settings → Beep in the last 3 seconds* turns the beeps off.
- [ ] With *Follow phone mode* and the phone on vibrate: the alert only vibrates.
- [ ] Alarm icon in the status bar during the rest: expected (exact alarm).

### 4. Recovery
- [ ] Mid-rest, close the app from recents (swipe it away). Reopen it: Home shows "Workout in progress". "Resume" goes back to the same set and the total time is still correct.
- [ ] Restart the phone mid-workout. Open the app and resume: the session continues and the total time includes the reboot period.
- [ ] Finish the workout → summary → pick an intensity (Low/Medium/High) and add a photo → keep "Update loads" checked → "Finish and save".

### 5. History
- [ ] The day is marked on the calendar. Tapping the day opens the session with sets, reps and loads.
- [ ] Toggle Week/Month in the summaries. Workouts, total time and frequency match what was done.
- [ ] The session row shows the intensity; the session detail shows the photo, and intensity and photo can be changed or removed there.
- [ ] "+ Activity" on a past day: log swimming (distance in m), indoor cycling (drawing at the top) and a "Free" one with your own name. They appear on that day, mark the calendar and count in Activities, Total time and the Sports card. Tap one to edit or delete it.
- [ ] Home: "Log activity" opens the same screen; the week counts the activity days and their time.
- [ ] **Live:** pick Running → *Start timer* → allow notifications and location. Walk outdoors for a few minutes with the screen locked: the notification counts up and shows the km. Pause/resume from the notification. *Finish*: the activity is saved with the time and distance and opens for intensity and photo.
- [ ] Start one, close the app from recents and reopen: Home shows "… in progress" with the right time.
- [ ] Indoor cycling or swimming: no GPS option, only the time.

### 6. Progress
- [ ] Log two assessments on different dates (edit the date at the top). Weight with a decimal comma.
- [ ] Compare the two: signed differences (+/−).
- [ ] Charts: weight and waist appear; change the metrics and the period. "All" shows every measure you have logged; tapping it again clears the selection.
- [ ] Photos: add two (front), one with an older date picked with *Date*, select both → "Compare selected"; delete one.
- [ ] Home and Measurements show weight and body fat (no waist) with their change.

### 6a. Reminders
- [ ] *Settings → Reminders → +*: pick a workout, a time 2 minutes ahead and today's weekday. At that time a "Time to work out" notification arrives, even with the screen locked; tapping it opens the app.
- [ ] A one-off reminder with "Other activity" (tap "Swimming" to fill the name) on a date: it fires once and then shows as off.
- [ ] Restart the phone: a weekly reminder still fires at its next time.
- [ ] Turning a reminder's switch off stops it.

### 7. Backup
- [ ] *Settings → Backup*: set the recovery password and store it in a password manager.
- [ ] Create a folder (e.g., `MyFit Backup`) synced with Google Drive by a sync app. Choose that folder in MyFit.
- [ ] "Back up now": a `myfit-backup-YYYYMMDD-HHmmss.myfit` file appears in the list, and in Drive after syncing.
- [ ] Edit something and wait about 10 min (or until the next day): a new file shows up on its own. At most 7 are kept in the folder.
- [ ] "Export backup to file" (e.g., to Downloads) works.
- [ ] **Restore on a clean install**: uninstall, reinstall, *Settings → Backup → Restore from a file* (or choose the folder and tap a backup) → confirm the date → type the password → workouts, history, measurements and photos come back.
- [ ] A wrong password shows "Wrong password" and nothing is changed.
- [ ] Delete the chosen folder and tap "Back up now": the status shows the error in red and a failure notification arrives.

### 8. Update
- [ ] Install a new version over the existing one (`adb install -r`): the data is kept.
- [ ] Updating from 1.2: workouts and history are intact, the library gains the new exercises and the existing predefined ones show their drawings (a predefined exercise you renamed keeps no drawing).
- [ ] Updating from 1.3: history is intact (old sessions have no intensity), and a backup made with 1.3 still restores.
- [ ] If you had changed any setting before 1.4, check *Settings → Rest end sound*: the new default (Always ring) only applies to settings never saved.

## Known issues
- The alarm icon appears in the status bar during the rest (`setAlarmClock`, chosen for its accuracy).
- With the app open, the end of the rest also shows the heads-up notification: that's what plays the sound.
- The folder picker may not list Google Drive directly. Use a local folder kept in sync by a sync app.
