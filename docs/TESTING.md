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
- [ ] Predefined exercises open read-only ("Exercício"): name, Muscle group with only its own group (not clickable) and the body map, Movement type with only its own type, "Each set counts" (Repetitions / Time, still editable), the drawing with its credit, "Watch how to do it" and the star. No other group/type options, photo picker, notes, Save or Delete.
- [ ] Switch a predefined exercise to "Tempo" and go back without saving anything: reopen it, it is still "Tempo", and a new workout with it asks for seconds. The name follows the app language (switch Portuguese ↔ English: Library, routines, workout and History change with it). A custom exercise keeps its name and photo, every field except notes, and Delete.
- [ ] Tap the star (top-right) on a predefined and on a custom exercise, go back: both are at the top of the Library and the picker, with a green star on the row; tap again to unstar. A new exercise has no star until saved.
- [ ] Activities: "Academia" / "Gym" is in the sport list (dumbbell icon), without distance.
- [ ] The library shows a drawing next to every predefined exercise. Open one: start → end drawing, the body map with its group in green, and "Watch how to do it" opens a YouTube search.
- [ ] In a custom exercise, change the muscle group: the body map updates at once. Pick a photo: it shows; remove it: the placeholder comes back.
- [ ] A custom exercise without a photo shows the body map in the list and during the workout.
- [ ] *Settings → Image credits* lists the illustrations and their licenses; *About* shows "Developed by luannamorim".

### 2. Execution (with the app open)
- [ ] Start: the first time, the app asks for notification permission. Accept it.
- [ ] The set screen shows the exercise drawing (or the body map); the play button opens the video search.
- [ ] Complete set → the rest starts on its own. Use +15 s and −15 s.
- [ ] Lower the reps with − (e.g. 12 → 8) and tap *Complete set*: while the rest fades in, the set still shows 8, never the planned 12 or the next set's number.
- [ ] A workout with a plank (or side plank): the set shows a time (e.g. 30 s) with ±. *Start* → the ring counts down, beeps in the last 3 seconds and plays the sound at the target; hold a few seconds more and tap *Done*: History shows the time actually held (e.g. 34 s). Cardio exercises show minutes.
- [ ] Updating from 1.5: planks and cardio in your workouts now show a time; check the targets.
- [ ] At the end of the rest: vibration, sound and a green pulse; "REST COMPLETE" appears.
- [ ] Adjust the load with +/− and complete the set: the next set inherits the load.
- [ ] Pause (the total time stops) and resume. Skip set and skip exercise; tap the skipped exercise in the top strip to go back to it.

### 3. Locked screen and background
- [ ] Complete a set and **lock the screen** during the rest: at the end, the phone rings and vibrates, and the notification shows "Rest complete".
- [ ] On the lock screen, the notification shows the timer and the "Skip rest" button works.
- [ ] With the phone on **vibrate** and *Settings → Rest end sound → Always ring* (the default): the alert rings anyway, at the alarm volume.
- [ ] The last 3 seconds of rest beep (also with the screen locked), then MyFit's own end sound plays, not the phone's notification sound. *Settings → Beep in the last 3 seconds* turns the beeps off.
- [ ] With *Follow phone mode* and the phone on vibrate: the alert only vibrates.
- [ ] Alarm icon in the status bar during the rest: expected (exact alarm).

### 4. Recovery
- [ ] Mid-rest, close the app from recents (swipe it away). Reopen it: Home shows "Workout in progress". "Resume" goes back to the same set and the total time is still correct.
- [ ] Restart the phone mid-workout. Open the app and resume: the session continues and the total time includes the reboot period.
- [ ] Finish the workout → summary → pick an intensity (Low/Medium/High) and add a photo → keep "Update loads" checked → "Finish and save".

### 5. History
- [ ] The day is marked on the calendar. Tapping the day opens the session with sets, reps and loads.
- [ ] Toggle Week/Month in the summaries. Workouts, total time and frequency match what was done.
- [ ] With more than 10 hours in the period (Month) and a bigger system font: "Tempo total" stays on one line (a bit smaller) and its card is as tall as the others.
- [ ] The session row shows the intensity; the session detail shows the photo, and intensity and photo can be changed or removed there.
- [ ] Home → *Start activity* (log it): swimming (distance in m), indoor cycling (drawing at the top) and a "Free" one with your own name, one of them on a past date. They appear on that day in History, mark the calendar and count in Activities, Total time and the Sports card. Tap one to edit or delete it. History has no "+ Activity" button and no "Exercises done" card.
- [ ] Home: "Start activity" opens the same screen; the week counts the activity days and their time.
- [ ] **Live:** pick Running → *Start timer* → allow notifications and location. Walk outdoors for a few minutes with the screen locked: the notification counts up and shows the km. Pause/resume from the notification. *Finish* pauses the timer and opens "Activity done" (time, distance, intensity, notes, photo); nothing is saved yet: back returns to the paused timer, *Save* logs it and closes it for good.
- [ ] Open a logged activity from History: the sport is fixed (no sport chips); date, time, distance, intensity and photo can be changed.
- [ ] Start one, close the app from recents and reopen: Home shows "… in progress" with the right time.
- [ ] Indoor cycling or swimming: no GPS option, only the time.

### 6. Progress
- [ ] Log two assessments on different dates (edit the date at the top). Weight with a decimal comma.
- [ ] Compare the two: signed differences (+/−).
- [ ] Charts open with every measure selected (no Height chip); change the metrics and the period. Tapping "All" clears the selection; tapping it again selects everything.
- [ ] Photos: add two (front), one with an older date picked with *Date*. Tap one: it opens full screen; swipe to the other, pinch and double-tap to zoom, ✕ or back to close. Press and hold one to select, tap the other → "Compare selected"; in the comparison, tap a photo to see it full screen and swipe to the other; delete one.
- [ ] Workout or activity photo: tapping it opens it full screen.
- [ ] Home and Measurements show weight on its own card, then body fat and BMI side by side (no waist), with their change.
- [ ] Height: type it once ("178", or "1,78" in meters, saved as 178 cm). BMI shows on that assessment and on older weigh-ins without a height.
- [ ] Body-fat estimate: *Settings → Profile → Sex*. An assessment with neck and abdomen (women: neck, waist and hips) and no typed body fat shows a body-fat value (estimates are not labeled); the editor shows the BMI and estimate line while typing. A typed body fat replaces the estimate. With Sex "Not set", only BMI appears.
- [ ] Status dot: in the top-right corner of the BMI and body-fat cards on Home and Progress, green for *Regular*, yellow for *Baixo*/*Alto*, red for *Muito baixo*/*Muito alto* (BMI 18,5–24,9 is green). Without Sex, body fat has no dot. In Portuguese on a small phone, the Settings chips (Sex, Theme, Rest sound) move whole to the next line instead of breaking their text.
- [ ] Tapping the BMI or body-fat card (Home or Progress, also when it shows "—") opens its ranges table (Muito baixo, Baixo, Regular, Alto, Muito alto, each word on one line) with the current band highlighted; body fat without Sex shows the men and women columns and "Set your sex". On Home, tapping weight still opens Progress.
- [ ] Height typed once comes filled in on the next new assessment; saving a new assessment without typing anything else is refused as empty.
- [ ] Body cards always present once there is an assessment: without height BMI shows "—" and "Height missing"; without Sex body fat shows "—" and "Set your sex" (in Portuguese "Falta a altura" / "Defina o sexo"). Body fat and BMI cards always have the same height, even when a label wraps (bigger font).

### 6a. Reminders
- [ ] Workouts tab: the bell next to + (outline, gray) opens Reminders. Home's header has only the settings gear.
- [ ] *Settings → Reminders → +*: pick a workout, a time 2 minutes ahead and today's weekday. At that time a "Time to work out" notification arrives, even with the screen locked; tapping it opens the app.
- [ ] A one-off reminder with "Other activity" on a date: saving asks for a sport until one is picked; "Free" shows a name field. It fires once and then shows as off.
- [ ] Home's "Next workout" with an activity reminder (e.g. Swimming today, 2 h ahead): the card shows the sport's icon and name, "Done … ago" (or "Not done yet") and *Start*, which opens the new activity screen on Swimming. Log the swim: the card moves to the next reminder. While the swim timer runs, the card skips it too. A swim logged on another day (e.g. Wednesday for a Saturday reminder) does not hide Saturday's.
- [ ] Updating from 1.17: an old reminder whose text is a sport ("Natação", "Corrida") shows on Home as that sport without editing it; other texts open in the editor as "Free" with their name.
- [ ] Restart the phone: a weekly reminder still fires at its next time.
- [ ] Turning a reminder's switch off stops it.

### 6b. Nutrition
- [ ] The tab bar shows Home, Workouts, History, Progress and Nutrition; the gear on Home opens Settings, and back returns to Home.
- [ ] *Settings → Profile*: set the birth date (no future dates; "Clear" removes it) and the goal. With weight, height, sex and birth date set, Nutrition shows a water target (35 ml per kg) and a calorie target; a card without a target shows only what was logged (e.g. "1.5 L", "1,240 kcal") and "Fill in: …", which opens Progress or Settings.
- [ ] Own targets: the pencil on the water card sets the day's target in liters (and the buttons), the pencil on the calories card the kcal target; "Recommended: …" stays under each bar, and "Use recommended (…)" goes back to it. Home's rings and the water reminder follow your target.
- [ ] Water: +250 ml, +500 ml and *Other* (type an amount) add up; the ↶ icon in the card's right corner removes the last one. The pencil next to the total edits the two buttons' amounts (e.g. 300 and 600 ml); the water reminder's button uses the first one.
- [ ] Meal: *Add meal*, describe it, tap *Ask AI*: the share sheet opens with the question ready; pick an AI app, read the answer, come back and type the calories, change the time, save. It shows in the list and the total; tap it to edit or delete.
- [ ] Meal photo: with an empty description tap *Photo to AI* › *Take photo*: the share sheet opens with the photo and the question; the AI app receives both. Again with *Choose from gallery* and a nutrition label photo. Type "2 fatias" first: the question ends with *Detalhes: "2 fatias".*
- [ ] Meal photo, app killed: in Developer options turn on *Don't keep activities*, tap *Photo to AI* › *Take photo*, shoot: back in MyFit the share sheet still opens with the photo and the question, and the description typed before is still there. Turn the option off again.
- [ ] Go to yesterday with ‹: its own meals and water; › never goes past today. A weigh-in today doesn't change yesterday's target.
- [ ] Back up and restore: meals, water, birth date and goal come back.
- [ ] History: a day with water or meals logged shows a Nutrition card (water and calories against the targets, ✓ when hit). The calendar shows up to three dots per day, always in the same order and color (workout green, calories on target orange, water on target blue), with a legend under it; there's no weekly/monthly Nutrition card. Calories count as hit at or under the target to lose fat, at or over to gain muscle, within 10 % to maintain.
- [ ] History summary (Week/Month): Workouts, Activities, Total time, Frequency, then Food on target and Water on target as "hits/days so far" (e.g. 3/4 on a Thursday), hidden when nothing was logged in the period.
- [ ] Home, "This week": three rows (workout, food, water icons at the start), a check on each day done in the row's color (green, orange, blue); today has a border, future days are faded. No summary line under it.
- [ ] *Settings → Appearance → Week starts on*: pick Sunday. Home's week starts with D (Sunday), History's calendar with SUN/DOM and the *Week* summary runs Sunday to Saturday (its dates above the tiles). Back to Monday restores both. A backup restore keeps the choice.
- [ ] Nutrition tab: the water and calorie cards show a drop and a flame before their titles, like Home's cards.
- [ ] Nutrition reminders: *Add time* asks for the meal's name together with the time; *Add* stays off until there's a name.
- [ ] Home, after two workouts (or a workout and an activity) today: the done card shows "Today: 2 workouts · 1 activity", the total time, and each one with its start time, latest first; tapping a line opens it. With only one, the card is as before.
- [ ] Home: "Nutrition today" (before "Body") shows two rings with the percent of the water and calorie targets; tapping one opens the Nutrition tab. Past the target the ring is full and shows e.g. "108%" (amber for calories when the goal is Lose).
- [ ] Home cards (water, calories, weight, body fat, BMI) have an icon before their title; on a small phone the calories card still shows "x of y kcal" beside its ring.
- [ ] Bell on the Nutrition tab → *Nutrition reminders*: add a meal time 2 minutes ahead (it gets a name by the hour, e.g. *Lunch*; tap the name to change it). At that time "Time to eat · <name>" arrives, even with the screen locked; *Log meal* opens the meal editor for today; tapping the notification opens the Nutrition tab.
- [ ] Water: turn it on with a window that includes the next few minutes (e.g. every 1 h, from now). The notification shows today's progress; *+250 ml* logs it without opening the app and the notification goes away. Once the day's water target is reached, no more water reminders that day.
- [ ] Restart the phone: meal and water reminders still fire. Turning them off stops them.

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
- [ ] Updating from 1.6: no exercise is starred, the library and routines are intact, and a backup made with 1.6 still restores.
- [ ] If you had changed any setting before 1.4, check *Settings → Rest end sound*: the new default (Always ring) only applies to settings never saved.

## Known issues
- The alarm icon appears in the status bar during the rest (`setAlarmClock`, chosen for its accuracy).
- With the app open, the end of the rest also shows the heads-up notification: that's what plays the sound.
- The folder picker may not list Google Drive directly. Use a local folder kept in sync by a sync app.
