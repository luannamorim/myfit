# MyFit — Implementation Plan

> **Progress:** all stages complete. MVP `v0.1.0` and version `v1.0.0` released on November 12, 2026. Deviations from the original plan: backup to a user-chosen folder (no Drive API, at the user's request); charts using Vico's synchronous model; restore without a marker file (images merged before the commit, orphans removed afterwards).

> **For agents:** SUB-SKILL: superpowers:executing-plans (native execution in this session), delegating self-contained tasks to subagents when useful. Steps use checkboxes (`- [x]`) for tracking.

**Goal:** a native, personal, offline Android app for building workouts, running them with a reliable timer, viewing history, tracking measurements/photos and making encrypted backups to a user-chosen folder.

**Architecture:** a single `:app` module, MVVM, layers by package (`ui/` → `domain/` ← `data/`). Room is the source of truth; the workout session engine is pure Kotlin operating on persisted timestamps; platform services (foreground service, exact alarm, WorkManager) live in `platform/`.

**Stack:** Kotlin 2.4.21 (AGP 9.5.1 built-in Kotlin), Compose BOM 2026.09.00, Material 3 + material3-adaptive, Navigation Compose 2.10.2 (type-safe), Hilt 2.60.1, Room 2.8.5 (KSP 2.3.13), DataStore 1.3.0, WorkManager 2.12.0, Coil 3.6.3, Vico 3.3.1 (`compose-m3`), Tink 1.24.0, kotlinx-serialization 1.13.0, documentfile. Tests: JUnit4, kotlinx-coroutines-test, Turbine 1.2.1, Robolectric 4.17, Roborazzi 1.79.0, work-testing.

**Spec:** [`SPEC.md`](SPEC.md)

## Global constraints

- Package/applicationId `io.github.luannamorim.myfit`; debug `applicationIdSuffix = ".debug"`. minSdk 26, compileSdk/targetSdk 37, JDK 21, Gradle 9.9.1.
- AGP 9: do **not** apply `org.jetbrains.kotlin.android`; use `kotlin { compilerOptions { … } }`; apply `org.jetbrains.kotlin.plugin.compose` and `org.jetbrains.kotlin.plugin.serialization` 2.4.21; Kotlin pinned via the root `buildscript` classpath.
- UI in English by default with a Brazilian Portuguese translation (strings in `res/values` and `res/values-pt-rBR`); metric units (kg, cm); locale-aware number and date formatting; decimal input accepts a comma or a period.
- No network: the manifest doesn't declare `INTERNET`. No `READ_MEDIA_*` permissions (Photo Picker).
- Session time always comes from a persisted `Mark` (Task 2.1); no UI counter is a source of truth.
- The dark theme is the default (`ThemeMode.DARK`). Green is the only accent color.
- Backup: password required before the first backup; keeps 7 files; versioned format (`formatVersion = 1`).
- Environment: `JAVA_HOME=~/.local/share/jdks/jdk-21.0.13+9`, `ANDROID_HOME=~/Android/Sdk` exported before every `./gradlew`. Tests run with `--add-exports/--add-opens` for `java.base` (required by Robolectric on SDK 36 + JDK 21, already in `app/build.gradle.kts`).
- Compose tests use `androidx.compose.ui.test.junit4.v2.*` (the v1 rules are deprecated).
- Standard verification for every task: `./gradlew testDebugUnitTest` green. At the end of each stage: `./gradlew lintDebug testDebugUnitTest recordRoborazziDebug` + visual review of the PNGs in `app/build/outputs/roborazzi/`.
- Conventional Commits, in English, **without** a `Co-Authored-By` trailer; push to `origin main` at the end of each stage.
- Starting with the `v0.1.0` APK, every Room schema change requires a `Migration` + `MigrationTestHelper`.

## Review focus

1. **Decimal load with a comma** ("42,5") typed on the PT-BR keyboard → accepted as 42.5; invalid text doesn't erase the previous value. Test in 3.2.
2. **Workout crossing midnight** (start 23:30, end 00:40) → counts on the start day in the calendar and in the summaries. Test in 5.1.
3. **System time changed or reboot mid-workout** → within the same boot, time uses the monotonic clock; after a reboot it uses the wall clock, with no negative duration. Test in 2.1 and 4.1.
4. **Workout or exercise edited/deleted with an active session or existing history** → the session continues and history keeps the original names/loads. Test in 4.2.
5. **Finishing with no completed sets** → doesn't save an empty session; offers to discard it. Test in 4.1/4.4.

---

## File structure

Code root: `app/src/main/java/io/github/luannamorim/myfit/` (below, `…/`). Tests mirror it in `app/src/test/java/io/github/luannamorim/myfit/`.

```
…/MyFitApp.kt                      Application (@HiltAndroidApp, Configuration.Provider)
…/MainActivity.kt                  single Activity, edge-to-edge, theme + NavHost
…/di/                              Hilt modules (Database, Time, Dispatchers, Bindings)
…/core/time/                       Mark, TimeSource, SystemTimeSource
…/core/format/                     DecimalInput, formatters (duration, kg, cm, locale-aware dates)
…/domain/model/                    domain models and enums
…/domain/session/                  SessionEngine (pure), ActiveSession and types
…/domain/history/                  HistoryCalculator (summaries, grouping by day)
…/domain/body/                     Measure, BodyComparison, chart series
…/domain/backup/                   v1 DTOs, BackupHeader, BackupCrypto, BackupArchive, Retention
…/data/db/                         MyFitDatabase, Converters, entity/, dao/, seed/ExerciseSeed.kt
…/data/repository/                 Exercise/Routine/Session/History/Body repositories + mappers
…/data/prefs/                      SettingsRepository, BackupStateStore (DataStore)
…/data/images/                     ImageStore
…/data/backup/                     BackupManager, BackupKeyStore, KeyWrapper, BackupDestination (SAF)
…/platform/session/                SessionController, WorkoutService, RestAlarmScheduler, receivers, SessionNotifications
…/platform/backup/                 BackupScheduler, BackupWorker, BackupDebounceWorker
…/ui/theme/                        Color, Type, Shape, Theme, Spacing
…/ui/components/                   design system components + MyFitMark
…/ui/navigation/                   Routes, TopLevelDestination, MyFitNavHost, ActiveSessionBar
…/ui/feature/{home,routines,library,workout,history,evolution,settings}/   Screen + ViewModel per screen
app/src/main/res/font/             Geist and Geist Mono (OFL)
app/schemas/                       exported Room schemas
docs/                              BRAND.md, ARCHITECTURE.md, TESTING.md, screenshots/
```

---

## Stage 0 — Foundation

### Task 0.1: Repository and base documentation

**Files:** `.gitignore`, `.gitattributes`, `SPEC.md`, `IMPLEMENTATION_PLAN.md`, `README.md`

- [x] `.gitignore`: standard Gradle/Android + `local.properties`, `*.jks`, `*.keystore`, `keystore.properties`, `/captures`, `.idea/`, `.kotlin/`.
- [x] `README.md` skeleton: name, one-liner, "in development" status, links to the SPEC and the plan.
- [x] Commit `docs: add spec, implementation plan and readme skeleton`.
- [x] `gh repo create myfit --private --source . --remote origin --push` (inside `~/my/myfit`, personal account). Verify: `git remote -v` shows `git@github.com:luannamorim/myfit.git` and `gh repo view --json visibility` = `PRIVATE`.

---

## Stage 1 — Project and design system

### Task 1.1: Gradle skeleton and empty app

**Files:** `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `gradle/wrapper/*`, `gradlew*`, `app/build.gradle.kts`, `app/proguard-rules.pro`, `app/src/main/AndroidManifest.xml`, `…/MyFitApp.kt`, `…/MainActivity.kt`, `app/src/main/res/xml/data_extraction_rules.xml`, `app/src/main/res/xml/backup_rules.xml`, Test: `…/AppLaunchTest.kt`

**Produces:** `MyFitApp : Application, Configuration.Provider` with `@Inject lateinit var workerFactory: HiltWorkerFactory`; `MainActivity : ComponentActivity` (`@AndroidEntryPoint`).

- [x] Generate the wrapper with `~/.local/share/gradle-dist/gradle-9.9.1/bin/gradle wrapper --gradle-version 9.9.1`.
- [x] Version catalog with the versions from the Stack; plugins: android-application, kotlin-compose, kotlin-serialization, ksp, hilt, roborazzi.
- [x] `app/build.gradle.kts`: `buildFeatures.compose = true`; `testOptions.unitTests.isIncludeAndroidResources = true`; `ksp { arg("room.schemaLocation", "$projectDir/schemas") }`; release with `isMinifyEnabled = true`, `isShrinkResources = true`, `proguard-android-optimize.txt`.
- [x] Manifest: `android:allowBackup="false"`, `dataExtractionRules` excluding everything (cloud and device-transfer), `fullBackupContent` excluding everything; remove `androidx.work.WorkManagerInitializer` via `tools:node="remove"` on the `InitializationProvider`.
- [x] `AppLaunchTest` test (Robolectric, `@HiltAndroidTest` + `HiltAndroidRule`, `@Config(application = HiltTestApplication::class)`): `ActivityScenario.launch(MainActivity::class.java)` reaches `RESUMED`.
- [x] Run `./gradlew assembleDebug testDebugUnitTest` → BUILD SUCCESSFUL.
- [x] Commit `build: scaffold android project with compose, hilt and room toolchain`.

### Task 1.2: Theme (colors, typography, shapes)

**Files:** `…/ui/theme/{Color,Type,Shape,Spacing,Theme}.kt`, `res/font/geist_*.ttf`, `docs/licenses/GEIST_OFL.txt`, Test: `…/ui/theme/ColorContrastTest.kt`, `…/ui/theme/ThemeScreenshotTest.kt`

**Produces:**
- `enum class ThemeMode { DARK, LIGHT, SYSTEM }`
- `@Immutable data class MyFitColors(background, surface, surfaceRaised, outline, textPrimary, textSecondary, textTertiary, accent, onAccent, accentMuted, danger, warning: Color)`; `val DarkColors`, `val LightColors`; `val LocalMyFitColors`.
- `object MyFitTheme { val colors: MyFitColors @Composable get; val type: MyFitType @Composable get }`; `@Composable fun MyFitTheme(mode: ThemeMode = ThemeMode.DARK, content: @Composable () -> Unit)` that also sets up the `MaterialTheme` (derived colorScheme).
- `MyFitType` with `display`, `headline`, `title`, `body`, `bodyStrong`, `label`, `caption`, `overline`, `numericHero`, `numericLarge`, `numeric` — all in Geist; the numeric ones with `fontFeatureSettings = "tnum"` (Geist Mono discarded in the visual review: wide comma and slashed zero).
- Shapes: `small = 10.dp`, `medium = 16.dp`, `large = 24.dp`, pill-shaped buttons.

- [x] Download Geist (official vercel/geist-font release, OFL) → Regular/Medium/SemiBold/Bold weights; license in `docs/licenses/GEIST_OFL.txt`.
- [x] Write `ColorContrastTest` with a `contrast(a: Color, b: Color): Double` function (WCAG) and asserts, for Dark and Light: `textPrimary/background ≥ 7.0`, `textSecondary/background ≥ 4.5`, `onAccent/accent ≥ 4.5`, `accent/background ≥ 3.0`.
- [x] Run → fails (the types don't exist). Implement the theme, tuning the hex values until it passes.
- [x] `ThemeScreenshotTest` (Roborazzi, `@GraphicsMode(NATIVE)`, `@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")`): color and typography catalog in both themes.
- [x] `./gradlew testDebugUnitTest recordRoborazziDebug` → passes; review the PNGs.
- [x] Commit `feat(ui): add myfit theme with color, type and shape tokens`.

### Task 1.3: Brand and base components

**Files:** `res/drawable/ic_myfit_mark.xml`, `res/drawable/ic_launcher_foreground.xml`, `res/drawable/ic_launcher_monochrome.xml`, `res/mipmap-anydpi/ic_launcher.xml` (+`_round`), `res/values/ic_launcher_background.xml`, `…/ui/components/*.kt`, `docs/BRAND.md`, Test: `…/ui/components/ComponentCatalogScreenshotTest.kt`

**Produces (signatures in `ui/components`):**
- `MyFitMark(modifier, size: Dp, accentDot: Boolean = true)`, `MyFitWordmark(modifier)`
- `MyFitCard(modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit)` — 1 dp `outline` border, no shadow
- `PrimaryButton(text, onClick, modifier, enabled = true, icon: ImageVector? = null)`, `SecondaryButton(...)`, `TextAction(...)`
- `StatTile(label: String, value: String, modifier, delta: String? = null, deltaPositive: Boolean? = null)`
- `NumberStepper(value: String, onDecrement, onIncrement, onValueClick, label: String, modifier)` — targets ≥ 56 dp
- `RestRing(progress: Float, remainingText: String, modifier)` — ring with an `outline` track and an `accent` arc
- `SectionHeader(title, modifier, action: (@Composable () -> Unit)? = null)`, `EmptyState(title, message, modifier, action: (@Composable () -> Unit)? = null)`, `FilterChipRow(options: List<T>, selected: Set<T>, label: (T) -> String, onToggle: (T) -> Unit)`, `ConfirmDialog(title, message, confirmText, onConfirm, onDismiss, destructive = false)`, `MyFitTopBar(title, onBack: (() -> Unit)?, actions)`

- [x] Draw the symbol: an "M" in a continuous stroke (rounded corners, uniform thickness) whose last segment rises above the first peak and ends in an `accent` dot. Icon: `#0B0B0C` background, white symbol; monochrome = stroke only.
- [x] `ComponentCatalogScreenshotTest`: all components in both themes + the icon rendered at 108 dp.
- [x] Run `recordRoborazziDebug`, review the PNGs, iterate until the mark is legible at 24 dp.
- [x] `docs/BRAND.md`: concept, palette (hex + usage), typography, spacing, components, do/don't.
- [x] Commit `feat(ui): add brand mark, launcher icon and base components`.

### Task 1.4: Navigation

**Files:** `…/ui/navigation/{Routes,TopLevelDestination,MyFitNavHost}.kt`, placeholder screens in `ui/feature/*/…Screen.kt`, Test: `…/ui/navigation/NavigationTest.kt`

**Produces:**
- `@Serializable` routes: `HomeRoute`, `RoutinesRoute`, `HistoryRoute`, `EvolutionRoute`, `SettingsRoute` (objects); `RoutineEditorRoute(routineId: Long? = null)`, `ExercisePickerRoute`, `ExerciseLibraryRoute`, `ExerciseEditorRoute(exerciseId: Long? = null)`, `WorkoutRoute`, `SessionSummaryRoute(sessionId: Long)`, `SessionDetailRoute(sessionId: Long)`, `AssessmentEditorRoute(assessmentId: Long? = null)`, `AssessmentCompareRoute`, `PhotosRoute`, `BackupRoute`.
- `enum class TopLevelDestination(route, labelRes, icon, selectedIcon)` in the order Home, Workouts, History, Progress, Settings.
- `MyFitNavHost(navController)` with `NavigationSuiteScaffold` (bar on compact, rail on medium/expanded) hidden on `WorkoutRoute`; per-tab back stack with `saveState/restoreState`.

- [x] `NavigationTest` (Compose + Robolectric): clicking each tab shows the screen title; going back from the Workouts tab returns to Home.
- [x] Implement; run the tests; compact (411 dp) and expanded (840 dp) screenshots.
- [x] Commit `feat(ui): add adaptive navigation shell with five top-level destinations`.
- [x] End of stage: standard stage verification + push.

---

## Stage 2 — Data

### Task 2.1: Reliable time

**Files:** `…/core/time/{Mark,TimeSource,SystemTimeSource}.kt`, `…/di/TimeModule.kt`, Test: `…/core/time/MarkTest.kt`

**Produces:**
- `data class Mark(val wallMs: Long, val elapsedMs: Long, val bootCount: Int)`
- `fun Mark.millisUntil(later: Mark): Long` — same `bootCount` ⇒ `later.elapsedMs - elapsedMs`; otherwise `later.wallMs - wallMs`; always `coerceAtLeast(0)`.
- `interface TimeSource { fun now(): Mark; fun zone(): ZoneId }`; `SystemTimeSource` (`System.currentTimeMillis`, `SystemClock.elapsedRealtime`, `Settings.Global.BOOT_COUNT`).
- `class FakeTimeSource(var current: Mark, var zoneId: ZoneId = ZoneId.of("America/Sao_Paulo")) : TimeSource` in `src/test` with `advance(ms)` (advances wall and elapsed), `jumpWall(ms)` (wall only), `reboot(wallMs)` (bootCount+1, elapsed=0).

- [x] Tests: `sameBoot_usesElapsed_evenIfWallJumpsBack` (wall −1h, elapsed +60 s ⇒ 60_000); `differentBoot_usesWall`; `neverNegative` (wall goes back after a reboot ⇒ 0).
- [x] Implement; run; commit `feat(core): add boot-aware time marks`.

### Task 2.2: Model, entities, DAOs and database

**Files:** `…/domain/model/*.kt`, `…/data/db/{MyFitDatabase,Converters}.kt`, `…/data/db/entity/*.kt`, `…/data/db/dao/*.kt`, `…/di/DatabaseModule.kt`, Tests: `…/data/db/{RoutineDaoTest,SessionDaoTest,BodyDaoTest}.kt`

**Produces:**
- Enums: `MuscleGroup { CHEST, BACK, SHOULDERS, BICEPS, TRICEPS, FOREARMS, ABS, QUADRICEPS, HAMSTRINGS, GLUTES, CALVES, FULL_BODY, CARDIO, OTHER }` (with `labelRes`), `MovementType { PUSH, PULL, LEGS, OTHER }`, `SessionStatus { ACTIVE, FINISHED }`, `ItemStatus { PENDING, DONE, SKIPPED }`, `PhotoPose { FRONT, SIDE, BACK, OTHER }`.
- `MarkEmbedded(wallMs, elapsedMs, bootCount)` for `@Embedded(prefix = …)`.
- Entities (table = name in snake_case):
  - `ExerciseEntity(id, name, muscleGroup, movementType, imagePath: String?, notes: String?, isCustom: Boolean, archived: Boolean, createdAtMs: Long)`
  - `RoutineEntity(id, name, description: String?, position: Int, createdAtMs, updatedAtMs)`
  - `RoutineExerciseEntity(id, routineId → routine CASCADE, exerciseId → exercise RESTRICT, position, sets: Int, reps: Int, restSeconds: Int, loadKg: Double?, notes: String?)`
  - `WorkoutSessionEntity(id, routineId: Long? → routine SET_NULL, routineName, status, started: MarkEmbedded, paused: MarkEmbedded?, totalPausedMs: Long, ended: MarkEmbedded?, durationMs: Long?, cursorExercise: Int, cursorSet: Int, restStarted: MarkEmbedded?, restDurationMs: Long?, startDateEpochDay: Long)`
  - `SessionExerciseEntity(id, sessionId → CASCADE, exerciseId: Long? → SET_NULL, position, name, muscleGroup, movementType, imagePath: String?, restSeconds: Int, notes: String?, status: ItemStatus)`
  - `SessionSetEntity(id, sessionExerciseId → CASCADE, setIndex, plannedReps, plannedLoadKg: Double?, actualReps: Int?, actualLoadKg: Double?, status: ItemStatus, completedAtWallMs: Long?)`
  - `BodyAssessmentEntity(id, dateEpochDay: Long, weightKg, bodyFatPct, neckCm, shouldersCm, chestCm, waistCm, abdomenCm, hipsCm, armRightCm, armLeftCm, forearmRightCm, forearmLeftCm, thighRightCm, thighLeftCm, calfRightCm, calfLeftCm: Double?, notes: String?, createdAtMs, updatedAtMs)`
  - `BodyPhotoEntity(id, dateEpochDay, imagePath, pose: PhotoPose, createdAtMs)`
- DAOs: `ExerciseDao`, `RoutineDao`, `SessionDao`, `BodyDao` (`Flow` queries for reads and `suspend` for writes; `@Transaction` for composite writes). Indexes on all FKs, `workout_session(status)`, `workout_session(startDateEpochDay)`, `body_assessment(dateEpochDay)`.
- `MyFitDatabase` version 1, `exportSchema = true`.

- [x] Tests (Robolectric, `Room.inMemoryDatabaseBuilder`): `deletingRoutine_cascadesRoutineExercises`; `deletingRoutine_keepsSessionWithNullRoutineId_andSnapshotName`; `deletingExerciseInUse_isRestricted`; `observeRoutineWithExercises_ordersByPosition`; `activeSession_returnsOnlyActive`; `assessments_orderedByDateDesc`.
- [x] Implement; run; confirm `app/schemas/…/1.json` is generated; commit `feat(data): add room schema for exercises, routines, sessions and body data`.

### Task 2.3: Predefined exercise library

**Files:** `…/data/db/seed/ExerciseSeed.kt`, Test: `…/data/db/seed/ExerciseSeedTest.kt`

**Produces:** `object ExerciseSeed { val items: List<SeedExercise> }` (`SeedExercise(name, muscleGroup, movementType)`), ~70 items in PT-BR (e.g., Supino reto com barra, Remada curvada, Remada unilateral, Pullover, Puxada frontal, Agachamento livre, Leg press 45°, Stiff, Elevação lateral, Rosca direta, Tríceps corda, Panturrilha em pé, Prancha, Esteira); inserted by `RoomDatabase.Callback.onCreate` via `ExerciseDao.insertAll`.

- [x] Tests: `seed_namesAreUnique_caseAndAccentInsensitive`; `seed_coversEveryMuscleGroupExceptOther`; `freshDatabase_containsSeed` (count = `items.size`, `isCustom = false`).
- [x] Implement; run; commit `feat(data): seed predefined exercise library`.

### Task 2.4: Workout and settings repositories

**Files:** `…/data/repository/{ExerciseRepository,RoutineRepository,Mappers}.kt`, `…/data/prefs/SettingsRepository.kt`, Tests: `…/data/repository/RoutineRepositoryTest.kt`, `…/data/prefs/SettingsRepositoryTest.kt`

**Produces:**
- Domain: `Exercise(id, name, muscleGroup, movementType, imagePath, notes, isCustom)`, `Routine(id, name, description, exercises: List<RoutineItem>)`, `RoutineItem(id, exercise: Exercise, sets, reps, restSeconds, loadKg: Double?, notes)`, `RoutineOverview(id, name, exerciseCount, lastDoneEpochDay: Long?)`.
- `ExerciseRepository`: `observeLibrary(query: String, groups: Set<MuscleGroup>): Flow<List<Exercise>>` (accent- and case-insensitive search, excludes `archived`), `get(id)`, `save(exercise): Long`, `delete(id)` (archives if referenced by a routine, otherwise deletes).
- `RoutineRepository`: `observeOverviews(): Flow<List<RoutineOverview>>`, `observe(id): Flow<Routine?>`, `save(routine: Routine): Long` (replaces items, positions 0..n−1), `duplicate(id): Long` (name `"<name> (copy)"`), `delete(id)`, `updateLoads(routineId, loads: Map<Long /*exerciseId*/, Double?>)`.
- `SettingsRepository`: `val settings: Flow<AppSettings>`; `AppSettings(themeMode = DARK, defaultRestSeconds = 90, restSound = RestSound.FOLLOW_RINGER, keepScreenOn = true, updateRoutineLoadsOnFinish = true, autoBackup = true)`; setters `suspend fun update(transform: (AppSettings) -> AppSettings)`; `exportJson(): String` / `importJson(json)` (used by the backup).
- Every "important" write (routine saved/duplicated/deleted) calls `BackupTrigger.onDataChanged()` — `interface BackupTrigger` defined here, implemented in 9.2 (until then, a no-op binding).

- [x] Tests: `save_assignsContiguousPositions`; `duplicate_copiesItemsWithNewIds_andSuffixName`; `librarySearch_ignoresAccentsAndCase` ("abdomen" finds "Abdômen"; "SUPINO" finds "Supino reto com barra"); `deleteExerciseInUse_archivesInsteadOfDeleting`; `settings_defaults` and `settings_exportImport_roundTrip`.
- [x] Implement; run; commit `feat(data): add exercise, routine and settings repositories`.

### Task 2.5: Image storage

**Files:** `…/data/images/ImageStore.kt`, Test: `…/data/images/ImageStoreTest.kt`

**Produces:** `class ImageStore(context, io: CoroutineDispatcher)`: `suspend fun import(uri: Uri, kind: ImageKind): String` (returns the relative path `images/<kind>/<uuid>.jpg`; decodes downscaled to a longest side of 1600 px with `BitmapFactory` (`inSampleSize`) + `ExifInterface` on all versions (decodes HEIF on API 28+; a single code path, testable on Robolectric); JPEG 85; writes `.tmp` and renames); `fun file(relativePath): File`; `suspend fun delete(relativePath)`; `suspend fun deleteOrphans(referenced: Set<String>): Int`. `enum class ImageKind(dir) { EXERCISE("exercises"), BODY("body") }`.

- [x] Tests (Robolectric, 4000×3000 bitmap written as `file://`): `import_downscalesLongestSideTo1600`; `import_returnsRelativePathUnderKindDir`; `deleteOrphans_removesOnlyUnreferenced`.
- [x] Implement; run; commit `feat(data): add local image store with downscaling`.
- [x] End of stage: standard stage verification + push.

---

## Stage 3 — Workouts

### Task 3.1: Workout list

**Files:** `…/ui/feature/routines/{RoutinesScreen,RoutinesViewModel}.kt`, Tests: `…/ui/feature/routines/RoutinesViewModelTest.kt`, `RoutinesScreenshotTest.kt`

**Consumes:** `RoutineRepository.observeOverviews/duplicate/delete`.
**Produces:** `RoutinesUiState(routines: List<RoutineOverview>, isLoading: Boolean)`; actions `onDuplicate(id)`, `onDelete(id)`; navigates to `RoutineEditorRoute(id|null)` and starts a workout via `onStart(id)` (wired up in 4.4).

- [x] VM test: `duplicate_addsCopyToList`; `delete_removesFromList` (with Turbine over a real in-memory repository).
- [x] Screen: list of `MyFitCard` (name, number of exercises, "done N days ago"), Start button on each card, menu (Edit, Duplicate, Delete with `ConfirmDialog`), "New workout" FAB, `EmptyState`.
- [x] Empty and 3-workout screenshots; commit `feat(routines): add routines list`.

### Task 3.2: Workout editor

**Files:** `…/ui/feature/routines/{RoutineEditorScreen,RoutineEditorViewModel,RoutineItemSheet}.kt`, `…/core/format/DecimalInput.kt`, Tests: `RoutineEditorViewModelTest.kt`, `…/core/format/DecimalInputTest.kt`, `RoutineEditorScreenTest.kt`

**Produces:**
- `object DecimalInput { fun parse(text: String): Double?; fun format(value: Double?): String }` — accepts `,` and `.`, up to 2 decimal places, rejects negatives and garbage; formats `42.5 → "42,5"`, `40.0 → "40"`.
- `RoutineEditorViewModel(savedStateHandle, repo)`: in-memory draft `RoutineDraft(name, description, items: List<DraftItem>)`; `addExercises(ids: List<Long>)` (defaults: 3 sets, 12 reps, `defaultRestSeconds`, null load), `remove(index)`, `move(from, to)`, `updateItem(index, sets, reps, restSeconds, loadKg, notes)`, `save(): Boolean` (false and a `nameRequired` error if the name is blank; allows 0 exercises).
- Picker result via `previousBackStackEntry.savedStateHandle["picked_exercise_ids"]: LongArray`.
- Drag-to-reorder with the `sh.calvin.reorderable` library (current stable version) and a dedicated handle.

- [x] `DecimalInputTest` tests: `"42,5"→42.5`, `"42.5"→42.5`, `" 40 "→40.0`, `"abc"→null`, `"-5"→null`, `format(42.5)=="42,5"`, `format(40.0)=="40"`.
- [x] VM tests: `save_withBlankName_fails`; `addExercises_usesDefaultRestFromSettings`; `move_reordersItems`; `save_persistsOrder`.
- [x] `RoutineEditorScreenTest`: add 2 exercises via a simulated result, remove 1, save → the repository contains 1 item.
- [x] Implement (item sheet with `NumberStepper` for sets/reps/rest in 15 s steps, load field with a decimal keyboard); screenshots; commit `feat(routines): add routine editor with reorder and per-exercise targets`.

### Task 3.3: Library, picker and custom exercise

**Files:** `…/ui/feature/library/{ExerciseLibraryScreen,ExercisePickerScreen,ExerciseEditorScreen,LibraryViewModel,ExerciseEditorViewModel}.kt`, Tests: `LibraryViewModelTest.kt`, `ExerciseEditorViewModelTest.kt`

**Consumes:** `ExerciseRepository`, `ImageStore`.
**Produces:** search + group `FilterChipRow`; multi-select picker that returns `picked_exercise_ids`; editor with name, group, movement type, notes and image (`PickVisualMedia(ImageOnly)` → `ImageStore.import` immediately).

- [x] Tests: `filterByGroup_andQuery_combine`; `editor_blankName_cannotSave`; `editor_replaceImage_deletesOldFile`.
- [x] Implement; library and editor screenshots; commit `feat(library): add exercise library, picker and custom exercise editor`.
- [x] End of stage: standard stage verification + push.

---

## Stage 4 — Workout execution

### Task 4.1: Session engine (pure)

**Files:** `…/domain/session/{ActiveSession,SessionEngine}.kt`, Test: `…/domain/session/SessionEngineTest.kt`

**Produces:**
```kotlin
data class ActiveSession(
    val id: Long, val routineId: Long?, val routineName: String,
    val startedAt: Mark, val pausedAt: Mark?, val totalPausedMs: Long,
    val exercises: List<SessionExercise>, val cursor: Cursor, val rest: Rest?,
)
data class SessionExercise(val id: Long, val exerciseId: Long?, val name: String, val imagePath: String?,
    val restSeconds: Int, val notes: String?, val status: ItemStatus, val sets: List<SessionSet>)
data class SessionSet(val id: Long, val plannedReps: Int, val plannedLoadKg: Double?,
    val actualReps: Int?, val actualLoadKg: Double?, val status: ItemStatus, val completedAtWallMs: Long?)
data class Cursor(val exerciseIndex: Int, val setIndex: Int)
data class Rest(val startedAt: Mark, val durationMs: Long)
sealed interface FinishCheck { data object Ok : FinishCheck; data object NothingCompleted : FinishCheck }
```
`object SessionEngine` (all pure, returning a new `ActiveSession`): `completeSet(s, reps, loadKg, now)`, `skipSet(s, now)`, `skipExercise(s, now)`, `goToExercise(s, index)`, `editSet(s, exerciseIndex, setIndex, reps, loadKg)`, `adjustRest(s, deltaMs, now)`, `setExerciseRest(s, exerciseIndex, seconds)`, `skipRest(s)`, `pause(s, now)`, `resume(s, now)`; queries `elapsedMs(s, now)`, `restRemainingMs(s, now): Long?`, `isComplete(s)`, `checkFinish(s): FinishCheck`, `restEndsAtWallMs(s, now): Long?`.

Rules:
- Completing a set marks it `DONE` with the actual values and moves the cursor: to the exercise's next `PENDING` set; otherwise to the next exercise with a `PENDING` set (in order, then back to earlier pending ones); an exercise with no pending sets becomes `DONE` (or `SKIPPED` if none of its sets is `DONE`).
- Rest starts after completing a set **unless** the session is now complete; duration = `restSeconds` of the completed set's exercise.
- Skip set = `SKIPPED`, no rest. Skip exercise = all pending sets `SKIPPED`, the cursor advances. `goToExercise` on a `SKIPPED` exercise reopens the skipped sets as `PENDING`.
- `pause`: freezes the total time and the rest (`rest` is re-anchored on `resume` with the remaining time). `elapsedMs = startedAt.millisUntil(now) − totalPausedMs − (current pause)`.
- `adjustRest` clamps the remaining time to ≥ 0; `skipRest` clears `rest`.

- [x] Tests (with `FakeTimeSource`): `completeSet_startsRestWithExerciseRest`; `completeLastSetOfExercise_movesToNextExercise_andRests`; `completeFinalSet_noRest_sessionComplete`; `skipSet_noRest`; `skipExercise_marksPendingSkipped_andAdvances`; `goToSkippedExercise_reopensSkippedSets`; `pauseResume_excludesPausedTimeFromElapsed`; `pauseDuringRest_freezesRemaining`; `adjustRest_plus15_minusBelowZeroClamps`; `elapsed_wallClockJumpSameBoot_unaffected`; `elapsed_afterReboot_usesWall`; `checkFinish_noDoneSets_returnsNothingCompleted`; `editSet_changesActualValues`.
- [x] Implement; run; commit `feat(session): add pure workout session engine`.

### Task 4.2: Session persistence

**Files:** `…/data/repository/SessionRepository.kt`, Test: `…/data/repository/SessionRepositoryTest.kt`

**Produces:** `SessionRepository`: `observeActive(): Flow<ActiveSession?>`, `getActive(): ActiveSession?`, `start(routineId: Long, now: Mark, zone: ZoneId): Long` (fails with `IllegalStateException` if there is already an active session or the routine has no exercises; creates snapshots), `save(session: ActiveSession)` (updates the session + all exercises/sets in one transaction), `finish(session, now, updateRoutineLoads: Boolean): Long` (status `FINISHED`, `ended`, `durationMs = elapsedMs`; updates the routine's loads with the load from each exercise's last `DONE` set; calls `BackupTrigger`), `discard(sessionId)`.

- [x] Tests: `start_snapshotsNamesAndTargets`; `start_withActiveSession_throws`; `start_emptyRoutine_throws`; `save_thenGetActive_roundTrips`; `routineDeletedDuringSession_sessionStillLoads`; `exerciseRenamedAfterFinish_historyKeepsOriginalName`; `finish_updatesRoutineLoadsWhenRequested`; `finish_storesDurationExcludingPauses`.
- [x] Implement; run; commit `feat(session): persist active session state at every step`.

### Task 4.3: Controller, alarm, notifications and service

**Files:** `…/platform/session/{SessionController,RestAlarmScheduler,RestAlarmReceiver,SessionActionReceiver,SessionNotifications,WorkoutService,WorkoutServiceStarter}.kt`, `AndroidManifest.xml`, Tests: `SessionControllerTest.kt`, `RestAlarmSchedulerTest.kt`, `SessionNotificationsTest.kt`

**Produces:**
- `SessionController` (`@Singleton`, a `Mutex` serializing operations): `val active: StateFlow<ActiveSession?>`; `suspend fun start(routineId): Long`, `completeSet(reps, loadKg)`, `skipSet()`, `skipExercise()`, `goToExercise(index)`, `editSet(...)`, `adjustRest(deltaSeconds)`, `setExerciseRest(index, seconds)`, `skipRest()`, `pause()`, `resume()`, `finish(updateRoutineLoads): Long`, `discard()`, `resync()`. Each operation: engine → `SessionRepository.save` → reschedule/cancel the alarm → update the notification.
- `interface RestAlarmScheduler { fun schedule(triggerAtWallMs: Long); fun cancel() }`; implementation with `AlarmManager.setAlarmClock` (falls back to `setAndAllowWhileIdle` if `!canScheduleExactAlarms()`).
- `RestAlarmReceiver`: posts "Rest complete" on the `rest_end` channel (high importance, notification sound + vibration) or `rest_end_alarm` (`USAGE_ALARM`), depending on `AppSettings.restSound`; `setTimeoutAfter(60_000)`.
- `SessionNotifications`: `session_ongoing` channel (low importance); notification with `setUsesChronometer` (`setChronometerCountDown` during rest), fixed text when paused, actions `ACTION_COMPLETE_SET` / `ACTION_SKIP_REST` → `SessionActionReceiver` (`goAsync` + controller); a `setDeleteIntent` that reposts the notification; `setOnlyAlertOnce`.
- `WorkoutService` (`foregroundServiceType="health"`): `ServiceCompat.startForeground(…, if (SDK ≥ 34) FOREGROUND_SERVICE_TYPE_HEALTH else 0)`; observes `active`; stops when it is `null`; `START_STICKY`; catches `ForegroundServiceStartNotAllowedException` → `stopSelf`.
- Manifest: `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_HEALTH`, `HIGH_SAMPLING_RATE_SENSORS`, `USE_EXACT_ALARM`, `SCHEDULE_EXACT_ALARM` (`maxSdkVersion="32"`), `VIBRATE`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.

- [x] Tests (Robolectric shadows): `completeSet_schedulesAlarmAtRestEnd` (`ShadowAlarmManager.nextScheduledAlarm.triggerAtTime == wall + 90_000`); `skipRest_cancelsAlarm`; `pause_cancelsAlarm_resume_reschedulesRemaining`; `restAlarmReceiver_postsRestEndNotification`; `actionReceiver_completeSet_advancesSession`; `resync_withActiveSession_startsService_andReschedules`; `finish_stopsService_andCancelsAlarm`.
- [x] Implement; run; commit `feat(session): add foreground service, rest alarm and notification actions`.

### Task 4.4: Execution screen and summary

**Files:** `…/ui/feature/workout/{WorkoutScreen,WorkoutViewModel,WorkoutUiState,SessionSummaryScreen,SessionSummaryViewModel}.kt`, Tests: `WorkoutViewModelTest.kt`, `WorkoutFlowTest.kt`, `WorkoutScreenshotTest.kt`

**Produces:** `WorkoutUiState(routineName, elapsedText, isPaused, exerciseName, imagePath, setLabel /* "Set 2 of 3" */, repsValue, loadValue, restRemainingText: String?, restProgress: Float?, exercises: List<ExerciseChip>, nextUp: String?, canFinish)`; a 250 ms ticker only while the screen is visible (`repeatOnLifecycle`), derived from `active` + `TimeSource`.

Screen (immersive, forced dark theme, `FLAG_KEEP_SCREEN_ON` if `keepScreenOn`):
- Top: workout name, total time (Geist Mono), pause/resume, menu (skip exercise, adjust the exercise's rest, end, discard).
- Center: exercise, image (if any), "Set X of Y", reps and load `NumberStepper` (steps of 1 and 2.5 kg), a "Complete set" button (1 tap) and "Skip set".
- Rest: `RestRing` with the remaining time, `−15 s`/`+15 s`, "Skip rest", next set/exercise; when it reaches zero with the app open: `accent` color pulse + vibration (+ its own sound if notifications are denied).
- Bottom strip with the exercises (done/current/pending/skipped), tap → `goToExercise`.
- Requests `POST_NOTIFICATIONS` the first time a workout is started.
- End: if `NothingCompleted` → "No sets completed. Discard workout?" dialog; otherwise `SessionSummaryRoute`, with a summary (duration, sets, total volume), an "Update workout loads" checkbox (default from `updateRoutineLoadsOnFinish`) and "Finish".

- [x] VM test: `restCountdown_derivesFromMarks_notTicks` (advance `FakeTimeSource` by 30 s without a tick ⇒ text "1:00"); `finishWithoutCompletedSets_emitsDiscardPrompt`.
- [x] `WorkoutFlowTest` (Compose + Robolectric, routine with 2 exercises × 2 sets): complete → rest visible → skip rest → … → end → summary shows 4 sets.
- [x] Wire `onStart(id)` from 3.1 to `controller.start` + navigation to `WorkoutRoute`.
- [x] Screenshots: set, rest, paused, summary; commit `feat(workout): add immersive workout screen and session summary`.

### Task 4.5: Session recovery

**Files:** `…/ui/navigation/ActiveSessionBar.kt`, `…/MainActivity.kt`, `…/MyFitApp.kt`, Test: `SessionRecoveryTest.kt`

**Produces:** `ActiveSessionBar(session, elapsedText, onResume)` shown above the navigation on every tab when there is an active session; `MyFitApp.onCreate` and `MainActivity.onStart` call `controller.resync()`.

- [x] Test: database with an `ACTIVE` session (rest already expired) → open `MainActivity` → "Workout in progress" bar visible; tap → `WorkoutRoute` at the same exercise/set, with no rest.
- [x] Implement; commit `feat(workout): resume in-progress session after app restart`.
- [x] End of stage: standard stage verification + push.

---

## Stage 5 — History

### Task 5.1: History calculations

**Files:** `…/domain/history/HistoryCalculator.kt`, Test: `HistoryCalculatorTest.kt`

**Produces:** `SessionSummary(id, routineName, startDate: LocalDate, startTime: LocalTime, durationMs, setsDone, exercises: List<String>)`; `PeriodSummary(sessions: Int, totalDurationMs: Long, avgPerWeek: Double, exercises: List<ExerciseFrequency(name, sessions)>)`; `object HistoryCalculator { fun summarize(items: List<SessionSummary>, range: ClosedRange<LocalDate>): PeriodSummary; fun daysWithSessions(items): Set<LocalDate>; fun weekOf(date): ClosedRange<LocalDate> /* Mon–Sun */; fun monthOf(yearMonth): ClosedRange<LocalDate> }`. `avgPerWeek = sessions / (days in range / 7.0)`.

- [x] Tests: `sessionCrossingMidnight_countsOnStartDate`; `summarize_week_countsAndTotals`; `avgPerWeek_forMonth_of30Days`; `exercises_sortedByFrequencyThenName`; `weekOf_sunday_returnsPreviousMonday`.
- [x] Implement; commit `feat(history): add weekly and monthly summary calculations`.

### Task 5.2: History queries

**Files:** `…/data/db/dao/SessionDao.kt`, `…/data/repository/HistoryRepository.kt`, Test: `HistoryRepositoryTest.kt`

**Produces:** `HistoryRepository`: `observeSummaries(range: ClosedRange<LocalDate>): Flow<List<SessionSummary>>` (`FINISHED` only), `observeDetail(sessionId): Flow<SessionDetail?>` (`SessionDetail(summary, exercises: List<SessionExerciseDetail(name, status, sets: List<SessionSet>)>)`), `deleteSession(id)`.

- [x] Tests: `summaries_excludeActive`; `summaries_filteredByRange`; `detail_returnsSetsInOrder`.
- [x] Implement; commit `feat(history): add history queries`.

### Task 5.3: History and detail screens

**Files:** `…/ui/feature/history/{HistoryScreen,HistoryViewModel,MonthCalendar,SessionDetailScreen,SessionDetailViewModel}.kt`, Tests: `HistoryViewModelTest.kt`, `HistoryScreenshotTest.kt`

**Produces:** `MonthCalendar(month: YearMonth, marked: Set<LocalDate>, selected: LocalDate?, today: LocalDate, onSelect, onMonthChange)` (Mon–Sun grid, swipe between months, workout day = `accent` dot/highlight); summary with a Week/Month toggle (workouts, total time, weekly frequency, exercises); list for the selected day → `SessionDetailRoute`; detail with exercises, sets (reps × load, skipped ones dimmed) and delete session.

- [x] VM test: `selectingDay_listsThatDaysSessions`; `toggleMonth_switchesSummaryRange`.
- [x] Screenshots: month with workouts, selected day, detail; commit `feat(history): add calendar, period summaries and session detail`.
- [x] End of stage: standard stage verification + push.

---

## Stage 6 — Home and MVP

### Task 6.1: Dashboard

**Files:** `…/ui/feature/home/{HomeScreen,HomeViewModel}.kt`, Tests: `HomeViewModelTest.kt`, `HomeScreenshotTest.kt`

**Produces:** `HomeUiState(greeting /* Good morning/Good afternoon/Good evening */, dateText /* "Thursday, November 12" */, active: ActiveSession?, today: List<SessionSummary>, suggested: RoutineOverview?, routines: List<RoutineOverview>, last: SessionSummary?, body: BodySnapshot?, week: WeekStrip(days: List<DayMark>, sessions, totalDurationMs))`. `BodySnapshot` comes from 7.1 (until then, `null`). Suggestion: a routine with exercises that was never done (lowest position), otherwise the one with the oldest `lastDoneEpochDay`.

- [x] Tests: `greeting_byHour` (5–11 morning, 12–17 afternoon, otherwise evening); `suggestion_prefersNeverDone_thenOldest`; `today_listsOnlyTodaysFinishedSessions`.
- [x] Screen in this order: greeting/date → active session (Resume) or today's workout → "Start" with the suggested workout → workouts (carousel) → last activity → body → week (7 dots, Mon–Sun). Empty and populated screenshots.
- [x] Commit `feat(home): add dashboard`.

### Task 6.2: Signed release and MVP v0.1.0

**Files:** `app/build.gradle.kts`, `app/proguard-rules.pro`, `README.md`

- [x] `keytool -genkeypair -v -keystore ~/my/.keystores/myfit-release.jks -alias myfit -keyalg RSA -keysize 4096 -validity 36500` with a random password; store `MYFIT_STORE_FILE`, `MYFIT_STORE_PASSWORD`, `MYFIT_KEY_ALIAS`, `MYFIT_KEY_PASSWORD` in `~/.gradle/gradle.properties` (outside the repo).
- [x] `signingConfigs.release` read from those properties; if they're missing, the release is built unsigned (the build doesn't break).
- [x] R8 rules for kotlinx.serialization (DTOs/routes), Hilt/Room (generated code), Tink.
- [x] `versionCode = 1`, `versionName = "0.1.0"`; `./gradlew assembleRelease` with no missing-class warnings; `apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk` → signed; size < 15 MB.
- [x] README: "Installation" section (flow for unverified developer apps / ADB) and "MVP status".
- [x] Commit `build: configure release signing and r8`; tag `v0.1.0`; `gh release create v0.1.0 --prerelease` with the `myfit-0.1.0.apk` APK; copy it to the local Downloads folder.
- [x] End of stage: standard stage verification + push (with tags).

---

## Stage 7 — Progress

### Task 7.1: Body domain

**Files:** `…/domain/body/{Measure,BodyComparison,BodySeries}.kt`, `…/domain/model/BodyAssessment.kt`, Test: `BodyComparisonTest.kt`

**Produces:** `BodyAssessment(id, date: LocalDate, values: Map<Measure, Double>, notes)`; `enum class Measure(labelRes, unit: MeasureUnit /* KG, PERCENT, CM */)` in the order WEIGHT, BODY_FAT, NECK, SHOULDERS, CHEST, WAIST, ABDOMEN, HIPS, ARM_RIGHT, ARM_LEFT, FOREARM_RIGHT, FOREARM_LEFT, THIGH_RIGHT, THIGH_LEFT, CALF_RIGHT, CALF_LEFT; `MeasureDiff(measure, from: Double?, to: Double?, diff: Double?)`; `object BodyComparison { fun compare(from: BodyAssessment, to: BodyAssessment): List<MeasureDiff>; fun series(items: List<BodyAssessment>, measure: Measure): List<Pair<LocalDate, Double>> }`; `BodySnapshot(latest: BodyAssessment, weightDelta: Double?, highlights: List<MeasureDiff>)` for Home.

- [x] Tests: `compare_diffIsToMinusFrom` (weight 71→73 = +2; waist 78→79 = +1); `compare_missingValue_diffNull`; `series_sortedByDate_skipsMissing`; `diffFormat_signed` ("+2 kg", "−1,5 cm", "0 cm").
- [x] Implement; commit `feat(body): add measures, comparison and series`.

### Task 7.2: Assessments (list and editor)

**Files:** `…/data/repository/BodyRepository.kt`, `…/ui/feature/evolution/{EvolutionScreen,EvolutionViewModel,AssessmentEditorScreen,AssessmentEditorViewModel}.kt`, Tests: `BodyRepositoryTest.kt`, `AssessmentEditorViewModelTest.kt`

**Produces:** `BodyRepository`: `observeAssessments(): Flow<List<BodyAssessment>>`, `get(id)`, `save(a): Long`, `delete(id)`, `observePhotos()`, `addPhoto(date, pose, relativePath)`, `deletePhoto(id)` (every write calls `BackupTrigger`). Progress tab with sections: Measurements (list with date and weight), Charts, Compare, Photos. Editor with a date (DatePicker) and fields grouped (General, Torso, Arms, Legs) using `DecimalInput`, prefilled as placeholders from the latest assessment.

- [x] Tests: `save_thenObserve_newestFirst`; `editor_requiresDate_only`; `editor_rejectsInvalidDecimal_keepsPrevious`; `home_bodySnapshot_usesLatestTwo`.
- [x] Wire `BodySnapshot` into Home; screenshots; commit `feat(evolution): add body assessments list and editor`.

### Task 7.3: Comparison and charts

**Files:** `…/ui/feature/evolution/{AssessmentCompareScreen,CompareViewModel,ChartsSection,ChartsViewModel}.kt`, Tests: `CompareViewModelTest.kt`, `ChartsViewModelTest.kt`, `EvolutionScreenshotTest.kt`

**Produces:** comparison with "From"/"To" selectors (default: first and last), a Metric | From | To | Difference table (signed difference in a neutral color, without judging whether going up or down is good); Vico `LineCartesianLayer` charts per metric, selected via `FilterChipRow` (default: Weight, Waist, Right arm), period All/1 year/6 months; one chart per selected metric (different units).

- [x] Tests: `compare_defaultsToFirstAndLast`; `charts_selectionKeptInViewModel`; `charts_periodFilter_trimsSeries`.
- [x] Screenshots: comparison and charts with 6 assessments; commit `feat(evolution): add assessment comparison and measure charts`.

### Task 7.4: Body photos

**Files:** `…/ui/feature/evolution/{PhotosScreen,PhotosViewModel,PhotoCompareScreen}.kt`, Test: `PhotosViewModelTest.kt`

**Produces:** grid grouped by date (newest first), add (date + pose + `PickVisualMedia` → `ImageStore.import(…, BODY)`), delete (removes the file), compare two photos side by side (select 2, same pose suggested) with their dates.

- [x] Tests: `photos_groupedByDateDesc`; `deletePhoto_removesFile`; `compare_requiresExactlyTwo`.
- [x] Screenshots; commit `feat(evolution): add body photos and side-by-side comparison`.
- [x] End of stage: standard stage verification + push.

---

## Stage 8 — Backup format

### Task 8.1: Data snapshot (v1 DTOs)

**Files:** `…/domain/backup/dto/BackupDataV1.kt`, `…/data/backup/SnapshotMapper.kt`, Test: `SnapshotMapperTest.kt`

**Produces:** `@Serializable data class BackupDataV1(exercises, routines, routineExercises, sessions, sessionExercises, sessionSets, assessments, photos: List<…Dto>)` — DTOs mirror the entities with primitive fields and stable names; `SnapshotMapper`: `suspend fun export(db): BackupDataV1`, `suspend fun replaceAll(db, data)` (a single `withTransaction`: deletes in child→parent order and inserts parent→child, preserving IDs), `fun referencedImages(data): Set<String>`, `fun counts(data): Map<String, Int>`.

- [x] Tests: `exportReplaceAll_roundTripsEveryTable` (populate every table, export, clear, reimport, compare lists); `json_isStable` (serialize → deserialize to an equal value; unknown fields ignored).
- [x] Commit `feat(backup): add versioned data snapshot`.

### Task 8.2: Encryption and header

**Files:** `…/domain/backup/{BackupHeader,BackupCrypto,KeyDerivation}.kt`, Test: `BackupCryptoTest.kt`

**Produces:**
- Binary header: `"MYFITBAK"` (8 B) | `headerVersion: u16 = 1` | `kdf: u8 = 1 (PBKDF2-HMAC-SHA256)` | `iterations: u32` | `salt: 16 B` | `kcv: 16 B` | `createdAtMs: u64`. `BackupHeader.read(input): BackupHeader` / `write(out)` / `bytes()`.
- `object KeyDerivation { fun derive(password: CharArray, salt: ByteArray, iterations: Int = 600_000): ByteArray /* 32 B, NFKC */; fun kcv(key): ByteArray /* HKDF-SHA256(key, info="myfit-kcv"), 16 B */ }`.
- `object BackupCrypto { fun encrypt(key, header, plaintext: InputStream, out: OutputStream); fun decrypt(key, input: InputStream, out: OutputStream): BackupHeader }` — Tink `AesGcmHkdfStreaming` (AES-256, HKDF-SHA256, 1 MiB segments), `associatedData = header.bytes()`. Errors: `BackupError.WrongPassword` (kcv ≠), `BackupError.Corrupted(reason)`, `BackupError.UnsupportedVersion(found)` (`sealed class BackupError : Exception`).

- [x] Tests (low iteration count via a parameter): `roundTrip_3MB`; `wrongPassword_detectedByKcv`; `flippedCiphertextByte_corrupted`; `tamperedHeaderCreatedAt_corrupted`; `truncatedFile_corrupted`; `badMagic_corrupted`; `headerVersion2_unsupported`.
- [x] Commit `feat(backup): add authenticated streaming encryption with password-derived key`.

### Task 8.3: Zip archive and manifest

**Files:** `…/domain/backup/{BackupArchive,BackupManifest}.kt`, Test: `BackupArchiveTest.kt`

**Produces:** `@Serializable BackupManifest(formatVersion = 1, appVersionName, appVersionCode, createdAtMs, dbSchemaVersion, counts: Map<String, Int>, entries: List<ManifestEntry(path, size, sha256)>)`; `object BackupArchive { fun write(out, data: BackupDataV1, settingsJson: String, images: Map<String, File>, meta: ManifestMeta); fun read(input, stagingDir: File, limits: ArchiveLimits = ArchiveLimits()): ArchiveContents(manifest, data, settingsJson, imageDir) }` — `data.json`/`settings.json` DEFLATED, images STORED; reading validates hashes, names (`..`, absolute paths, outside `images/`), a maximum of 20,000 entries and 2 GiB uncompressed, `formatVersion ≤ 1`.

- [x] Tests: `writeRead_roundTrip_withImages`; `hashMismatch_rejected`; `zipSlipEntry_rejected`; `missingManifest_rejected`; `newerFormatVersion_rejected`; `entryNotInManifest_rejected`.
- [x] Commit `feat(backup): add backup archive with manifest and integrity checks`.

### Task 8.4: Recovery password and stored key

**Files:** `…/data/backup/{BackupKeyStore,KeyWrapper,AndroidKeystoreKeyWrapper}.kt`, `…/ui/feature/settings/BackupPasswordDialog.kt`, Test: `BackupKeyStoreTest.kt`

**Produces:** `interface KeyWrapper { fun wrap(plain: ByteArray): ByteArray; fun unwrap(wrapped: ByteArray): ByteArray }` (Keystore AES-GCM, alias `myfit_backup_wrap`, no auth, no `setUnlockedDeviceRequired`, prefixed IV); `BackupKeyStore(dataStore/file, wrapper, timeSource)`: `suspend fun setPassword(password: CharArray)` (generates a salt, derives the key, stores `salt`, `iterations`, `kcv` and the wrapped key), `suspend fun key(): StoredKey?` (`null` if not set or if unwrapping fails → "password required" status), `suspend fun unlock(password): Boolean` (re-derives with the saved salt and checks the kcv), `hasPassword(): Flow<Boolean>`. Dialog: password + confirmation, minimum 8 characters, warning "without the password there is no way to restore".

- [x] Tests (with `FakeKeyWrapper`): `setPassword_thenKey_returnsSameKey`; `unwrapFailure_returnsNull_andUnlockRestores`; `unlock_wrongPassword_false`.
- [x] Commit `feat(backup): store password-derived key wrapped by android keystore`.

### Task 8.5: Create and restore orchestration

**Files:** `…/data/backup/BackupManager.kt`, Test: `BackupManagerTest.kt`

**Produces:** `BackupManager`: `suspend fun create(out: OutputStream): BackupInfo` (snapshot → temporary zip in `cacheDir` → encrypts to `out`; requires the key), `suspend fun inspect(input: InputStream): BackupHeader` (reads only the header → date for confirmation), `suspend fun restore(input: InputStream, password: CharArray?): RestoreResult`, `suspend fun recoverInterruptedRestore()`. `sealed interface RestoreResult { Success(counts); WrongPassword; Corrupted(reason); UnsupportedVersion; InsufficientSpace; SessionActive }`.

Restore: active session ⇒ `SessionActive`; free space < 2× the size ⇒ `InsufficientSpace`; decrypts to `cacheDir` → `BackupArchive.read` into `filesDir/restore_staging` → cancels backup workers → writes the `filesDir/restore.pending` marker → `SnapshotMapper.replaceAll` → `images/` becomes `images_old`, staging becomes `images` → imports settings → removes `images_old` and the marker → validates counts and that every image reference exists → `resync` of alarms. `recoverInterruptedRestore` (called in `MyFitApp.onCreate`): if the marker exists, completes the directory swap.

- [x] Tests (Robolectric, real database + files): `createRestore_roundTrip_restoresDbImagesAndSettings`; `restore_wrongPassword_leavesLocalDataUntouched`; `restore_corrupted_leavesLocalDataUntouched`; `restore_withActiveSession_refused`; `recoverInterruptedRestore_completesSwap`.
- [x] Commit `feat(backup): add backup creation and atomic restore`.

### Task 8.6: Manual backup and restore from file (UI)

**Files:** `…/ui/feature/settings/{BackupScreen,BackupViewModel}.kt`, Test: `BackupViewModelTest.kt`

**Produces:** Backup screen with: password (set/change), "Export backup" (`CreateDocument("application/octet-stream")`, name `myfit-backup-YYYYMMDD-HHmmss.myfit`), "Restore from file" (`OpenDocument`) → shows the backup date (`inspect`) → destructive `ConfirmDialog` → asks for the password if `key()` is null or if the file's salt differs → result shown as a clear message for each `RestoreResult` case.

- [x] VM tests: `export_withoutPassword_promptsPasswordFirst`; `restore_showsBackupDateBeforeConfirm`; `restoreResult_wrongPassword_message`.
- [x] Commit `feat(backup): add manual export and restore from file`.
- [x] End of stage: standard stage verification + push.

---

## Stage 9 — Automatic folder backup

### Task 9.1: Folder destination and retention

**Files:** `…/data/backup/{BackupDestination,SafBackupDestination,FileBackupDestination}.kt`, `…/domain/backup/Retention.kt`, Tests: `RetentionTest.kt`, `FolderBackupWriterTest.kt`

**Produces:**
- `interface BackupDestination { suspend fun list(): List<BackupFileInfo>; suspend fun write(name: String, block: suspend (OutputStream) -> Unit): BackupFileInfo; suspend fun open(file: BackupFileInfo): InputStream; suspend fun delete(file: BackupFileInfo) }`; `BackupFileInfo(id: String, name: String, sizeBytes: Long, createdAtMs: Long)` (date extracted from the name).
- `SafBackupDestination(context, treeUri)` with `DocumentFile`: writes `<name>.partial`, renames it (fallback: writes directly to the final name if `renameTo` fails), throws `BackupError.FolderUnavailable` on `SecurityException`/missing folder. `FileBackupDestination(dir)` for tests.
- `object Retention { val NAME_REGEX = Regex("""myfit-backup-(\d{8})-(\d{6})\.myfit"""); fun toDelete(files: List<BackupFileInfo>, keep: Int = 7): List<BackupFileInfo> }` — ignores files that don't match the pattern.
- `FolderBackupWriter(manager, destination)`: `suspend fun run(): BackupFileInfo` → writes, rereads the header and compares the size, then deletes the extra files.

- [x] Tests: `toDelete_keepsNewest7`; `toDelete_ignoresForeignFiles`; `run_writesValidatesThenPrunes` (9 existing → 7 afterwards); `run_validationFails_doesNotPrune`.
- [x] Commit `feat(backup): write backups to user folder with retention`.

### Task 9.2: Scheduling

**Files:** `…/platform/backup/{BackupScheduler,BackupWorker,BackupDebounceWorker}.kt`, `…/data/prefs/BackupStateStore.kt`, Tests: `BackupWorkerTest.kt`, `BackupSchedulerTest.kt`

**Produces:** `BackupState(folderUri: String?, folderName: String?, dirtySinceMs: Long?, lastSuccessMs: Long?, lastFileName: String?, lastAttemptMs: Long?, lastError: BackupFailure?)`, `enum BackupFailure { NO_FOLDER, FOLDER_UNAVAILABLE, NO_PASSWORD, NO_SPACE, UNKNOWN }`; `BackupScheduler : BackupTrigger`: `onDataChanged()` (marks dirty + `enqueueUniqueWork("backup-debounce", REPLACE, delay 10 min)`), `ensurePeriodic()` (`"backup-daily"`, 24 h, `KEEP`, `requiresBatteryNotLow`, `requiresStorageNotLow`), `runNow()` (`"backup-run"` expedited with `force = true`). `BackupDebounceWorker` only enqueues `"backup-run"` with `KEEP`. `BackupWorker(force)`: if `!force && dirtySinceMs == null` ⇒ `success` without writing; if `autoBackup` is off and `!force` ⇒ no write; success clears dirty (only if `dirtySinceMs` didn't change during the run) and saves `lastSuccessMs`; failure saves `lastError`, keeps dirty, sends a notification (`backup_status` channel) and returns `failure` (no endless retries for `FOLDER_UNAVAILABLE`/`NO_PASSWORD`; `retry` with backoff for `UNKNOWN`).

- [x] Tests (`TestListenableWorkerBuilder` + `FileBackupDestination`): `clean_notForced_skips`; `dirty_writes_andClearsDirty`; `changeDuringRun_keepsDirty`; `folderUnavailable_setsError_keepsDirty_noRetry`; `noPassword_setsError`; `onDataChanged_enqueuesDebounce`.
- [x] Wire the real `BackupTrigger` in Hilt; `ensurePeriodic()` in `MyFitApp.onCreate`; commit `feat(backup): schedule daily and change-triggered backups`.

### Task 9.3: Automatic backup screen

**Files:** `…/ui/feature/settings/{BackupScreen,BackupViewModel}.kt`, Test: `BackupScreenshotTest.kt`

**Produces:** "Backup folder" section (choose/change via `OpenDocumentTree` + `takePersistableUriPermission` read/write; folder name), status (last backup "today at 14:32", last error in `danger` with a corrective action), automatic toggle, "Back up now" (progress via `WorkInfo`), list of the backups in the folder (date, size) → restore flow from 8.6, hint "use a sync app to keep this folder in Drive".

- [x] Screenshots: no folder, OK, with error; commit `feat(backup): add automatic folder backup settings and restore list`.
- [x] End of stage: standard stage verification + push.

---

## Stage 10 — Polish

### Task 10.1: Settings

**Files:** `…/ui/feature/settings/{SettingsScreen,SettingsViewModel}.kt`, Test: `SettingsViewModelTest.kt`

**Produces:** sections Appearance (theme), Workout (default rest, rest sound, keep screen on, update loads when finishing), Data (Backup →, Exercise library →), Battery (shows whether optimization is active; opens `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`; text about "Unrestricted" on Samsung), About (version, font licenses).

- [x] Tests: `themeChange_persists`; `defaultRest_clampedTo15to600`.
- [x] Commit `feat(settings): add preferences screen`.

### Task 10.2: Quality review

- [x] Empty and loading states on every screen; animations (`animateContentSize`, 200 ms fade on route transitions, rest with `animateFloatAsState`).
- [x] Screenshots of every screen in: dark, light, 840 dp (rail), 1.5× font; fix clipping and contrast.
- [x] Accessibility: `contentDescription` on action icons, targets ≥ 48 dp, timers with `semantics { liveRegion }` only at the end of the rest.
- [x] Weekly scheduled cleanup of orphan images (lightweight worker) using `ImageStore.deleteOrphans`.
- [x] `./gradlew lintDebug` with no errors and no relevant new warnings; clean `assembleRelease`.
- [x] Commits `fix(ui): …` / `refactor: …` per topic; push.

---

## Stage 11 — Documentation and release

### Task 11.1: Documentation

**Files:** `README.md`, `docs/ARCHITECTURE.md`, `docs/TESTING.md`, `docs/BRAND.md`, `docs/screenshots/*.png`

- [x] README: what it is, screenshots, features, stack, APK installation (including developer verification in Brazil, as checked on that date), folder backup + Drive sync, development (toolchain, Gradle commands, tests, screenshots), structure, font license.
- [x] ARCHITECTURE: layers, database schema, session engine and time model, platform (service/alarm/notifications), backup format (header, zip, encryption, atomic restore), decisions and trade-offs.
- [x] TESTING: manual checklist on the phone covering the SPEC's completion criteria (installation, workout with the screen locked, kill the app and resume, restart the phone mid-workout, calendar, measurements/charts, folder backup, restore on a clean install, loss of folder permission, Samsung "Unrestricted").
- [x] Commit `docs: add architecture, testing guide and final readme`.

### Task 11.2: Release v1.0.0

- [x] `versionCode = 10`, `versionName = "1.0.0"`; `./gradlew lintDebug testDebugUnitTest assembleRelease`; `apksigner verify`; size < 15 MB.
- [x] Commit `chore: release 1.0.0`; tag `v1.0.0`; `gh release create v1.0.0` with `myfit-1.0.0.apk` and release notes in PT-BR; copy it to the local Downloads folder; push with tags.
