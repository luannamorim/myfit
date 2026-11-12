# MyFit — Plano de Implementação

> **Para agentes:** SUB-SKILL: superpowers:executing-plans (execução nativa nesta sessão), delegando tarefas autocontidas a subagentes quando útil. Passos usam checkbox (`- [ ]`) para acompanhamento.

**Objetivo:** app Android nativo, pessoal e offline para montar treinos, executá-los com cronômetro confiável, ver histórico, acompanhar medidas/fotos e fazer backup criptografado numa pasta escolhida pelo usuário.

**Arquitetura:** um módulo `:app`, MVVM, camadas por pacote (`ui/` → `domain/` ← `data/`). Room é a fonte da verdade; o motor da sessão de treino é Kotlin puro operando sobre marcas temporais persistidas; serviços de plataforma (foreground service, alarme exato, WorkManager) ficam em `platform/`.

**Stack:** Kotlin 2.4.21 (Kotlin embutido do AGP 9.5.1), Compose BOM 2026.09.00, Material 3 + material3-adaptive, Navigation Compose 2.10.2 (type-safe), Hilt 2.60.1, Room 2.8.5 (KSP 2.3.13), DataStore 1.3.0, WorkManager 2.12.0, Coil 3.6.3, Vico 3.3.1 (`compose-m3`), Tink 1.24.0, kotlinx-serialization 1.13.0, documentfile. Testes: JUnit4, kotlinx-coroutines-test, Turbine 1.2.1, Robolectric 4.17, Roborazzi 1.79.0, work-testing.

**Spec:** [`SPEC.md`](SPEC.md)

## Restrições globais

- Pacote/applicationId `io.github.luannamorim.myfit`; debug `applicationIdSuffix = ".debug"`. minSdk 26, compileSdk/targetSdk 37, JDK 21, Gradle 9.9.1.
- AGP 9: **não** aplicar `org.jetbrains.kotlin.android`; usar `kotlin { compilerOptions { … } }`; aplicar `org.jetbrains.kotlin.plugin.compose` e `org.jetbrains.kotlin.plugin.serialization` 2.4.21; Kotlin fixado via classpath do `buildscript` raiz.
- Interface 100% PT-BR via `res/values/strings.xml`; unidades kg/cm; decimal exibido com vírgula, entrada aceita vírgula ou ponto.
- Sem rede: o manifest não declara `INTERNET`. Sem permissões `READ_MEDIA_*` (Photo Picker).
- Tempo de sessão vem sempre de `Mark` persistido (Tarefa 2.1); nenhum contador de UI é fonte de verdade.
- Tema escuro é o padrão (`ThemeMode.DARK`). Verde é a única cor de destaque.
- Backup: senha obrigatória antes do primeiro backup; mantém 7 arquivos; formato versionado (`formatVersion = 1`).
- Ambiente: `JAVA_HOME=~/.local/share/jdks/jdk-21.0.13+9`, `ANDROID_HOME=~/Android/Sdk` exportados antes de cada `./gradlew`. Testes rodam com `--add-exports/--add-opens` de `java.base` (exigência do Robolectric no SDK 36 + JDK 21, já no `app/build.gradle.kts`).
- Testes Compose usam `androidx.compose.ui.test.junit4.v2.*` (as regras v1 estão obsoletas).
- Verificação padrão de toda tarefa: `./gradlew testDebugUnitTest` verde. No fim de cada etapa: `./gradlew lintDebug testDebugUnitTest recordRoborazziDebug` + revisão visual dos PNGs em `app/build/outputs/roborazzi/`.
- Commits Conventional Commits, em inglês, **sem** trailer `Co-Authored-By`; push para `origin main` no fim de cada etapa.
- A partir do APK `v0.1.0`, toda mudança de schema Room exige `Migration` + `MigrationTestHelper`.

## Foco de revisão

1. **Carga decimal com vírgula** ("42,5") digitada no teclado PT-BR → aceita como 42.5; texto inválido não apaga o valor anterior. Teste em 3.2.
2. **Treino que atravessa meia-noite** (início 23:30, fim 00:40) → conta no dia de início no calendário e nos resumos. Teste em 5.1.
3. **Hora do sistema ajustada ou reboot no meio do treino** → no mesmo boot o tempo usa o relógio monotônico; após reboot usa o relógio de parede, sem duração negativa. Teste em 2.1 e 4.1.
4. **Treino ou exercício editado/excluído com sessão ativa ou histórico existente** → sessão continua e histórico mantém nomes/cargas originais. Teste em 4.2.
5. **Finalizar sem nenhuma série concluída** → não salva sessão vazia; oferece descartar. Teste em 4.1/4.4.

---

## Estrutura de arquivos

Raiz do código: `app/src/main/java/io/github/luannamorim/myfit/` (abaixo, `…/`). Testes espelham em `app/src/test/java/io/github/luannamorim/myfit/`.

```
…/MyFitApp.kt                      Application (@HiltAndroidApp, Configuration.Provider)
…/MainActivity.kt                  Activity única, edge-to-edge, tema + NavHost
…/di/                              módulos Hilt (Database, Time, Dispatchers, Bindings)
…/core/time/                       Mark, TimeSource, SystemTimeSource
…/core/format/                     DecimalInput, formatadores (duração, kg, cm, datas pt-BR)
…/domain/model/                    modelos de domínio e enums
…/domain/session/                  SessionEngine (puro), ActiveSession e tipos
…/domain/history/                  HistoryCalculator (resumos, agrupamento por dia)
…/domain/body/                     Measure, BodyComparison, séries de gráfico
…/domain/backup/                   DTOs v1, BackupHeader, BackupCrypto, BackupArchive, Retention
…/data/db/                         MyFitDatabase, Converters, entity/, dao/, seed/ExerciseSeed.kt
…/data/repository/                 Exercise/Routine/Session/History/Body repositórios + mapeadores
…/data/prefs/                      SettingsRepository, BackupStateStore (DataStore)
…/data/images/                     ImageStore
…/data/backup/                     BackupManager, BackupKeyStore, KeyWrapper, BackupDestination (SAF)
…/platform/session/                SessionController, WorkoutService, RestAlarmScheduler, receivers, SessionNotifications
…/platform/backup/                 BackupScheduler, BackupWorker, BackupDebounceWorker
…/ui/theme/                        Color, Type, Shape, Theme, Spacing
…/ui/components/                   componentes do design system + MyFitMark
…/ui/navigation/                   Routes, TopLevelDestination, MyFitNavHost, ActiveSessionBar
…/ui/feature/{home,routines,library,workout,history,evolution,settings}/   Screen + ViewModel por tela
app/src/main/res/font/             Geist e Geist Mono (OFL)
app/schemas/                       schemas exportados do Room
docs/                              BRAND.md, ARCHITECTURE.md, TESTING.md, screenshots/
```

---

## Etapa 0 — Fundação

### Tarefa 0.1: Repositório e documentação base

**Arquivos:** `.gitignore`, `.gitattributes`, `SPEC.md`, `IMPLEMENTATION_PLAN.md`, `README.md`

- [ ] `.gitignore`: Gradle/Android padrão + `local.properties`, `*.jks`, `*.keystore`, `keystore.properties`, `/captures`, `.idea/`, `.kotlin/`.
- [ ] `README.md` esqueleto: nome, uma linha, status "em desenvolvimento", link para SPEC e plano.
- [ ] Commit `docs: add spec, implementation plan and readme skeleton`.
- [ ] `gh repo create myfit --private --source . --remote origin --push` (dentro de `~/my/myfit`, conta pessoal). Verificar: `git remote -v` mostra `git@github.com:luannamorim/myfit.git` e `gh repo view --json visibility` = `PRIVATE`.

---

## Etapa 1 — Projeto e design system

### Tarefa 1.1: Esqueleto Gradle e app vazio

**Arquivos:** `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `gradle/wrapper/*`, `gradlew*`, `app/build.gradle.kts`, `app/proguard-rules.pro`, `app/src/main/AndroidManifest.xml`, `…/MyFitApp.kt`, `…/MainActivity.kt`, `app/src/main/res/xml/data_extraction_rules.xml`, `app/src/main/res/xml/backup_rules.xml`, Teste: `…/AppLaunchTest.kt`

**Produz:** `MyFitApp : Application, Configuration.Provider` com `@Inject lateinit var workerFactory: HiltWorkerFactory`; `MainActivity : ComponentActivity` (`@AndroidEntryPoint`).

- [ ] Gerar wrapper com `~/.local/share/gradle-dist/gradle-9.9.1/bin/gradle wrapper --gradle-version 9.9.1`.
- [ ] Version catalog com as versões da Stack; plugins: android-application, kotlin-compose, kotlin-serialization, ksp, hilt, roborazzi.
- [ ] `app/build.gradle.kts`: `buildFeatures.compose = true`; `testOptions.unitTests.isIncludeAndroidResources = true`; `ksp { arg("room.schemaLocation", "$projectDir/schemas") }`; release com `isMinifyEnabled = true`, `isShrinkResources = true`, `proguard-android-optimize.txt`.
- [ ] Manifest: `android:allowBackup="false"`, `dataExtractionRules` excluindo tudo (cloud e device-transfer), `fullBackupContent` excluindo tudo; remover `androidx.work.WorkManagerInitializer` via `tools:node="remove"` no `InitializationProvider`.
- [ ] Teste `AppLaunchTest` (Robolectric, `@HiltAndroidTest` + `HiltAndroidRule`, `@Config(application = HiltTestApplication::class)`): `ActivityScenario.launch(MainActivity::class.java)` chega a `RESUMED`.
- [ ] Rodar `./gradlew assembleDebug testDebugUnitTest` → BUILD SUCCESSFUL.
- [ ] Commit `build: scaffold android project with compose, hilt and room toolchain`.

### Tarefa 1.2: Tema (cores, tipografia, formas)

**Arquivos:** `…/ui/theme/{Color,Type,Shape,Spacing,Theme}.kt`, `res/font/geist_*.ttf`, `docs/licenses/GEIST_OFL.txt`, Teste: `…/ui/theme/ColorContrastTest.kt`, `…/ui/theme/ThemeScreenshotTest.kt`

**Produz:**
- `enum class ThemeMode { DARK, LIGHT, SYSTEM }`
- `@Immutable data class MyFitColors(background, surface, surfaceRaised, outline, textPrimary, textSecondary, textTertiary, accent, onAccent, accentMuted, danger, warning: Color)`; `val DarkColors`, `val LightColors`; `val LocalMyFitColors`.
- `object MyFitTheme { val colors: MyFitColors @Composable get; val type: MyFitType @Composable get }`; `@Composable fun MyFitTheme(mode: ThemeMode = ThemeMode.DARK, content: @Composable () -> Unit)` que também monta o `MaterialTheme` (colorScheme derivado).
- `MyFitType` com `display`, `headline`, `title`, `body`, `bodyStrong`, `label`, `caption`, `overline`, `numericHero`, `numericLarge`, `numeric` — tudo em Geist; os numéricos com `fontFeatureSettings = "tnum"` (Geist Mono descartada na revisão visual: vírgula larga e zero cortado).
- Formas: `small = 10.dp`, `medium = 16.dp`, `large = 24.dp`, botões em pílula.

- [ ] Baixar Geist (release oficial vercel/geist-font, OFL) → pesos Regular/Medium/SemiBold/Bold; licença em `docs/licenses/GEIST_OFL.txt`.
- [ ] Escrever `ColorContrastTest` com função `contrast(a: Color, b: Color): Double` (WCAG) e asserts, para Dark e Light: `textPrimary/background ≥ 7.0`, `textSecondary/background ≥ 4.5`, `onAccent/accent ≥ 4.5`, `accent/background ≥ 3.0`.
- [ ] Rodar → falha (tipos não existem). Implementar o tema calibrando os hex até passar.
- [ ] `ThemeScreenshotTest` (Roborazzi, `@GraphicsMode(NATIVE)`, `@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")`): catálogo de cores e tipografia nos dois temas.
- [ ] `./gradlew testDebugUnitTest recordRoborazziDebug` → passa; revisar PNGs.
- [ ] Commit `feat(ui): add myfit theme with color, type and shape tokens`.

### Tarefa 1.3: Marca e componentes base

**Arquivos:** `res/drawable/ic_myfit_mark.xml`, `res/drawable/ic_launcher_foreground.xml`, `res/drawable/ic_launcher_monochrome.xml`, `res/mipmap-anydpi/ic_launcher.xml` (+`_round`), `res/values/ic_launcher_background.xml`, `…/ui/components/*.kt`, `docs/BRAND.md`, Teste: `…/ui/components/ComponentCatalogScreenshotTest.kt`

**Produz (assinaturas em `ui/components`):**
- `MyFitMark(modifier, size: Dp, accentDot: Boolean = true)`, `MyFitWordmark(modifier)`
- `MyFitCard(modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit)` — borda 1 dp `outline`, sem sombra
- `PrimaryButton(text, onClick, modifier, enabled = true, icon: ImageVector? = null)`, `SecondaryButton(...)`, `TextAction(...)`
- `StatTile(label: String, value: String, modifier, delta: String? = null, deltaPositive: Boolean? = null)`
- `NumberStepper(value: String, onDecrement, onIncrement, onValueClick, label: String, modifier)` — alvos ≥ 56 dp
- `RestRing(progress: Float, remainingText: String, modifier)` — anel com trilho `outline` e arco `accent`
- `SectionHeader(title, modifier, action: (@Composable () -> Unit)? = null)`, `EmptyState(title, message, modifier, action: (@Composable () -> Unit)? = null)`, `FilterChipRow(options: List<T>, selected: Set<T>, label: (T) -> String, onToggle: (T) -> Unit)`, `ConfirmDialog(title, message, confirmText, onConfirm, onDismiss, destructive = false)`, `MyFitTopBar(title, onBack: (() -> Unit)?, actions)`

- [ ] Desenhar o símbolo: "M" em traço contínuo (cantos arredondados, espessura uniforme) cujo último segmento sobe acima do primeiro pico e termina num ponto `accent`. Ícone: fundo `#0B0B0C`, símbolo branco; monocromático = só o traço.
- [ ] `ComponentCatalogScreenshotTest`: todos os componentes nos dois temas + ícone renderizado em 108 dp.
- [ ] Rodar `recordRoborazziDebug`, revisar PNGs, iterar até a marca ficar legível em 24 dp.
- [ ] `docs/BRAND.md`: conceito, paleta (hex + uso), tipografia, espaçamentos, componentes, do/don't.
- [ ] Commit `feat(ui): add brand mark, launcher icon and base components`.

### Tarefa 1.4: Navegação

**Arquivos:** `…/ui/navigation/{Routes,TopLevelDestination,MyFitNavHost}.kt`, telas placeholder em `ui/feature/*/…Screen.kt`, Teste: `…/ui/navigation/NavigationTest.kt`

**Produz:**
- Rotas `@Serializable`: `HomeRoute`, `RoutinesRoute`, `HistoryRoute`, `EvolutionRoute`, `SettingsRoute` (objects); `RoutineEditorRoute(routineId: Long? = null)`, `ExercisePickerRoute`, `ExerciseLibraryRoute`, `ExerciseEditorRoute(exerciseId: Long? = null)`, `WorkoutRoute`, `SessionSummaryRoute(sessionId: Long)`, `SessionDetailRoute(sessionId: Long)`, `AssessmentEditorRoute(assessmentId: Long? = null)`, `AssessmentCompareRoute`, `PhotosRoute`, `BackupRoute`.
- `enum class TopLevelDestination(route, labelRes, icon, selectedIcon)` na ordem Início, Treinos, Histórico, Evolução, Ajustes.
- `MyFitNavHost(navController)` com `NavigationSuiteScaffold` (barra no compacto, rail no médio/expandido) escondido em `WorkoutRoute`; back stack por aba com `saveState/restoreState`.

- [ ] `NavigationTest` (Compose + Robolectric): clicar em cada aba mostra o título da tela; voltar da aba Treinos retorna ao Início.
- [ ] Implementar; rodar testes; screenshots compacto (411 dp) e expandido (840 dp).
- [ ] Commit `feat(ui): add adaptive navigation shell with five top-level destinations`.
- [ ] Fim da etapa: verificação padrão de etapa + push.

---

## Etapa 2 — Dados

### Tarefa 2.1: Tempo confiável

**Arquivos:** `…/core/time/{Mark,TimeSource,SystemTimeSource}.kt`, `…/di/TimeModule.kt`, Teste: `…/core/time/MarkTest.kt`

**Produz:**
- `data class Mark(val wallMs: Long, val elapsedMs: Long, val bootCount: Int)`
- `fun Mark.millisUntil(later: Mark): Long` — mesmo `bootCount` ⇒ `later.elapsedMs - elapsedMs`; senão `later.wallMs - wallMs`; sempre `coerceAtLeast(0)`.
- `interface TimeSource { fun now(): Mark; fun zone(): ZoneId }`; `SystemTimeSource` (`System.currentTimeMillis`, `SystemClock.elapsedRealtime`, `Settings.Global.BOOT_COUNT`).
- `class FakeTimeSource(var current: Mark, var zoneId: ZoneId = ZoneId.of("America/Sao_Paulo")) : TimeSource` em `src/test` com `advance(ms)` (avança wall e elapsed), `jumpWall(ms)` (só wall), `reboot(wallMs)` (bootCount+1, elapsed=0).

- [ ] Testes: `sameBoot_usesElapsed_evenIfWallJumpsBack` (wall −1h, elapsed +60 s ⇒ 60_000); `differentBoot_usesWall`; `neverNegative` (wall volta após reboot ⇒ 0).
- [ ] Implementar; rodar; commit `feat(core): add boot-aware time marks`.

### Tarefa 2.2: Modelo, entidades, DAOs e banco

**Arquivos:** `…/domain/model/*.kt`, `…/data/db/{MyFitDatabase,Converters}.kt`, `…/data/db/entity/*.kt`, `…/data/db/dao/*.kt`, `…/di/DatabaseModule.kt`, Testes: `…/data/db/{RoutineDaoTest,SessionDaoTest,BodyDaoTest}.kt`

**Produz:**
- Enums: `MuscleGroup { CHEST, BACK, SHOULDERS, BICEPS, TRICEPS, FOREARMS, ABS, QUADRICEPS, HAMSTRINGS, GLUTES, CALVES, FULL_BODY, CARDIO, OTHER }` (com `labelRes`), `MovementType { PUSH, PULL, LEGS, OTHER }`, `SessionStatus { ACTIVE, FINISHED }`, `ItemStatus { PENDING, DONE, SKIPPED }`, `PhotoPose { FRONT, SIDE, BACK, OTHER }`.
- `MarkEmbedded(wallMs, elapsedMs, bootCount)` para `@Embedded(prefix = …)`.
- Entidades (tabela = nome em snake_case):
  - `ExerciseEntity(id, name, muscleGroup, movementType, imagePath: String?, notes: String?, isCustom: Boolean, archived: Boolean, createdAtMs: Long)`
  - `RoutineEntity(id, name, description: String?, position: Int, createdAtMs, updatedAtMs)`
  - `RoutineExerciseEntity(id, routineId → routine CASCADE, exerciseId → exercise RESTRICT, position, sets: Int, reps: Int, restSeconds: Int, loadKg: Double?, notes: String?)`
  - `WorkoutSessionEntity(id, routineId: Long? → routine SET_NULL, routineName, status, started: MarkEmbedded, paused: MarkEmbedded?, totalPausedMs: Long, ended: MarkEmbedded?, durationMs: Long?, cursorExercise: Int, cursorSet: Int, restStarted: MarkEmbedded?, restDurationMs: Long?, startDateEpochDay: Long)`
  - `SessionExerciseEntity(id, sessionId → CASCADE, exerciseId: Long? → SET_NULL, position, name, muscleGroup, movementType, imagePath: String?, restSeconds: Int, notes: String?, status: ItemStatus)`
  - `SessionSetEntity(id, sessionExerciseId → CASCADE, setIndex, plannedReps, plannedLoadKg: Double?, actualReps: Int?, actualLoadKg: Double?, status: ItemStatus, completedAtWallMs: Long?)`
  - `BodyAssessmentEntity(id, dateEpochDay: Long, weightKg, bodyFatPct, neckCm, shouldersCm, chestCm, waistCm, abdomenCm, hipsCm, armRightCm, armLeftCm, forearmRightCm, forearmLeftCm, thighRightCm, thighLeftCm, calfRightCm, calfLeftCm: Double?, notes: String?, createdAtMs, updatedAtMs)`
  - `BodyPhotoEntity(id, dateEpochDay, imagePath, pose: PhotoPose, createdAtMs)`
- DAOs: `ExerciseDao`, `RoutineDao`, `SessionDao`, `BodyDao` (consultas em `Flow` para leitura e `suspend` para escrita; `@Transaction` para gravações compostas). Índices em todas as FKs, `workout_session(status)`, `workout_session(startDateEpochDay)`, `body_assessment(dateEpochDay)`.
- `MyFitDatabase` versão 1, `exportSchema = true`.

- [ ] Testes (Robolectric, `Room.inMemoryDatabaseBuilder`): `deletingRoutine_cascadesRoutineExercises`; `deletingRoutine_keepsSessionWithNullRoutineId_andSnapshotName`; `deletingExerciseInUse_isRestricted`; `observeRoutineWithExercises_ordersByPosition`; `activeSession_returnsOnlyActive`; `assessments_orderedByDateDesc`.
- [ ] Implementar; rodar; confirmar `app/schemas/…/1.json` gerado; commit `feat(data): add room schema for exercises, routines, sessions and body data`.

### Tarefa 2.3: Biblioteca de exercícios pré-definida

**Arquivos:** `…/data/db/seed/ExerciseSeed.kt`, Teste: `…/data/db/seed/ExerciseSeedTest.kt`

**Produz:** `object ExerciseSeed { val items: List<SeedExercise> }` (`SeedExercise(name, muscleGroup, movementType)`), ~70 itens em PT-BR (ex.: Supino reto com barra, Remada curvada, Remada unilateral, Pullover, Puxada frontal, Agachamento livre, Leg press 45°, Stiff, Elevação lateral, Rosca direta, Tríceps corda, Panturrilha em pé, Prancha, Esteira); inserido por `RoomDatabase.Callback.onCreate` via `ExerciseDao.insertAll`.

- [ ] Testes: `seed_namesAreUnique_caseAndAccentInsensitive`; `seed_coversEveryMuscleGroupExceptOther`; `freshDatabase_containsSeed` (contagem = `items.size`, `isCustom = false`).
- [ ] Implementar; rodar; commit `feat(data): seed predefined exercise library`.

### Tarefa 2.4: Repositórios de treino e configurações

**Arquivos:** `…/data/repository/{ExerciseRepository,RoutineRepository,Mappers}.kt`, `…/data/prefs/SettingsRepository.kt`, Testes: `…/data/repository/RoutineRepositoryTest.kt`, `…/data/prefs/SettingsRepositoryTest.kt`

**Produz:**
- Domínio: `Exercise(id, name, muscleGroup, movementType, imagePath, notes, isCustom)`, `Routine(id, name, description, exercises: List<RoutineItem>)`, `RoutineItem(id, exercise: Exercise, sets, reps, restSeconds, loadKg: Double?, notes)`, `RoutineOverview(id, name, exerciseCount, lastDoneEpochDay: Long?)`.
- `ExerciseRepository`: `observeLibrary(query: String, groups: Set<MuscleGroup>): Flow<List<Exercise>>` (busca sem acento/caixa, exclui `archived`), `get(id)`, `save(exercise): Long`, `delete(id)` (arquiva se referenciado por rotina, senão apaga).
- `RoutineRepository`: `observeOverviews(): Flow<List<RoutineOverview>>`, `observe(id): Flow<Routine?>`, `save(routine: Routine): Long` (substitui itens, posições 0..n−1), `duplicate(id): Long` (nome `"<nome> (cópia)"`), `delete(id)`, `updateLoads(routineId, loads: Map<Long /*exerciseId*/, Double?>)`.
- `SettingsRepository`: `val settings: Flow<AppSettings>`; `AppSettings(themeMode = DARK, defaultRestSeconds = 90, restSound = RestSound.FOLLOW_RINGER, keepScreenOn = true, updateRoutineLoadsOnFinish = true, autoBackup = true)`; setters `suspend fun update(transform: (AppSettings) -> AppSettings)`; `exportJson(): String` / `importJson(json)` (usados no backup).
- Toda escrita "importante" (rotina salva/duplicada/excluída) chama `BackupTrigger.onDataChanged()` — `interface BackupTrigger` definida aqui, implementação em 9.2 (até lá, binding no-op).

- [ ] Testes: `save_assignsContiguousPositions`; `duplicate_copiesItemsWithNewIds_andSuffixName`; `librarySearch_ignoresAccentsAndCase` ("abdomen" encontra "Abdômen"; "SUPINO" encontra "Supino reto com barra"); `deleteExerciseInUse_archivesInsteadOfDeleting`; `settings_defaults` e `settings_exportImport_roundTrip`.
- [ ] Implementar; rodar; commit `feat(data): add exercise, routine and settings repositories`.

### Tarefa 2.5: Armazenamento de imagens

**Arquivos:** `…/data/images/ImageStore.kt`, Teste: `…/data/images/ImageStoreTest.kt`

**Produz:** `class ImageStore(context, io: CoroutineDispatcher)`: `suspend fun import(uri: Uri, kind: ImageKind): String` (retorna caminho relativo `images/<kind>/<uuid>.jpg`; decodifica reduzido para lado máx. 1600 px com `BitmapFactory` (`inSampleSize`) + `ExifInterface` em todas as versões (decodifica HEIF no API 28+; um único caminho, testável no Robolectric); JPEG 85; grava `.tmp` e renomeia); `fun file(relativePath): File`; `suspend fun delete(relativePath)`; `suspend fun deleteOrphans(referenced: Set<String>): Int`. `enum class ImageKind(dir) { EXERCISE("exercises"), BODY("body") }`.

- [ ] Testes (Robolectric, bitmap 4000×3000 gravado como `file://`): `import_downscalesLongestSideTo1600`; `import_returnsRelativePathUnderKindDir`; `deleteOrphans_removesOnlyUnreferenced`.
- [ ] Implementar; rodar; commit `feat(data): add local image store with downscaling`.
- [ ] Fim da etapa: verificação padrão de etapa + push.

---

## Etapa 3 — Treinos

### Tarefa 3.1: Lista de treinos

**Arquivos:** `…/ui/feature/routines/{RoutinesScreen,RoutinesViewModel}.kt`, Testes: `…/ui/feature/routines/RoutinesViewModelTest.kt`, `RoutinesScreenshotTest.kt`

**Consome:** `RoutineRepository.observeOverviews/duplicate/delete`.
**Produz:** `RoutinesUiState(routines: List<RoutineOverview>, isLoading: Boolean)`; ações `onDuplicate(id)`, `onDelete(id)`; navega para `RoutineEditorRoute(id|null)` e inicia treino via `onStart(id)` (ligado em 4.4).

- [ ] Teste VM: `duplicate_addsCopyToList`; `delete_removesFromList` (com Turbine sobre repositório real em memória).
- [ ] Tela: lista de `MyFitCard` (nome, nº de exercícios, "feito há N dias"), botão Iniciar por card, menu (Editar, Duplicar, Excluir com `ConfirmDialog`), FAB "Novo treino", `EmptyState`.
- [ ] Screenshots vazio e com 3 treinos; commit `feat(routines): add routines list`.

### Tarefa 3.2: Editor de treino

**Arquivos:** `…/ui/feature/routines/{RoutineEditorScreen,RoutineEditorViewModel,RoutineItemSheet}.kt`, `…/core/format/DecimalInput.kt`, Testes: `RoutineEditorViewModelTest.kt`, `…/core/format/DecimalInputTest.kt`, `RoutineEditorScreenTest.kt`

**Produz:**
- `object DecimalInput { fun parse(text: String): Double?; fun format(value: Double?): String }` — aceita `,` e `.`, até 2 casas, rejeita negativos e lixo; formata `42.5 → "42,5"`, `40.0 → "40"`.
- `RoutineEditorViewModel(savedStateHandle, repo)`: rascunho em memória `RoutineDraft(name, description, items: List<DraftItem>)`; `addExercises(ids: List<Long>)` (defaults: 3 séries, 12 reps, `defaultRestSeconds`, carga nula), `remove(index)`, `move(from, to)`, `updateItem(index, sets, reps, restSeconds, loadKg, notes)`, `save(): Boolean` (falso e erro `nameRequired` se nome em branco; permite 0 exercícios).
- Resultado do seletor via `previousBackStackEntry.savedStateHandle["picked_exercise_ids"]: LongArray`.
- Reordenação por arrastar com a biblioteca `sh.calvin.reorderable` (versão estável atual) e alça dedicada.

- [ ] Testes `DecimalInputTest`: `"42,5"→42.5`, `"42.5"→42.5`, `" 40 "→40.0`, `"abc"→null`, `"-5"→null`, `format(42.5)=="42,5"`, `format(40.0)=="40"`.
- [ ] Testes VM: `save_withBlankName_fails`; `addExercises_usesDefaultRestFromSettings`; `move_reordersItems`; `save_persistsOrder`.
- [ ] `RoutineEditorScreenTest`: adicionar 2 exercícios via resultado simulado, remover 1, salvar → repositório contém 1 item.
- [ ] Implementar (sheet de item com `NumberStepper` para séries/reps/descanso em passos de 15 s, campo de carga com teclado decimal); screenshots; commit `feat(routines): add routine editor with reorder and per-exercise targets`.

### Tarefa 3.3: Biblioteca, seletor e exercício personalizado

**Arquivos:** `…/ui/feature/library/{ExerciseLibraryScreen,ExercisePickerScreen,ExerciseEditorScreen,LibraryViewModel,ExerciseEditorViewModel}.kt`, Testes: `LibraryViewModelTest.kt`, `ExerciseEditorViewModelTest.kt`

**Consome:** `ExerciseRepository`, `ImageStore`.
**Produz:** busca + `FilterChipRow` de grupos; seletor multi-seleção que devolve `picked_exercise_ids`; editor com nome, grupo, tipo de movimento, observações e imagem (`PickVisualMedia(ImageOnly)` → `ImageStore.import` imediatamente).

- [ ] Testes: `filterByGroup_andQuery_combine`; `editor_blankName_cannotSave`; `editor_replaceImage_deletesOldFile`.
- [ ] Implementar; screenshots biblioteca e editor; commit `feat(library): add exercise library, picker and custom exercise editor`.
- [ ] Fim da etapa: verificação padrão de etapa + push.

---

## Etapa 4 — Execução do treino

### Tarefa 4.1: Motor da sessão (puro)

**Arquivos:** `…/domain/session/{ActiveSession,SessionEngine}.kt`, Teste: `…/domain/session/SessionEngineTest.kt`

**Produz:**
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
`object SessionEngine` (todas puras, retornam nova `ActiveSession`): `completeSet(s, reps, loadKg, now)`, `skipSet(s, now)`, `skipExercise(s, now)`, `goToExercise(s, index)`, `editSet(s, exerciseIndex, setIndex, reps, loadKg)`, `adjustRest(s, deltaMs, now)`, `setExerciseRest(s, exerciseIndex, seconds)`, `skipRest(s)`, `pause(s, now)`, `resume(s, now)`; consultas `elapsedMs(s, now)`, `restRemainingMs(s, now): Long?`, `isComplete(s)`, `checkFinish(s): FinishCheck`, `restEndsAtWallMs(s, now): Long?`.

Regras:
- Concluir série marca `DONE` com valores realizados e move o cursor: próxima série `PENDING` do exercício; senão próximo exercício com série `PENDING` (ordem, depois volta aos anteriores pendentes); exercício sem pendentes vira `DONE` (ou `SKIPPED` se nenhuma série `DONE`).
- Descanso inicia após concluir série **exceto** se a sessão ficou completa; duração = `restSeconds` do exercício da série concluída.
- Pular série = `SKIPPED`, sem descanso. Pular exercício = todas as pendentes `SKIPPED`, cursor avança. `goToExercise` em exercício `SKIPPED` reabre as séries puladas como `PENDING`.
- `pause`: congela tempo total e descanso (`rest` reancorado no `resume` com o restante). `elapsedMs = startedAt.millisUntil(now) − totalPausedMs − (pausa corrente)`.
- `adjustRest` limita o restante a ≥ 0; `skipRest` zera `rest`.

- [ ] Testes (com `FakeTimeSource`): `completeSet_startsRestWithExerciseRest`; `completeLastSetOfExercise_movesToNextExercise_andRests`; `completeFinalSet_noRest_sessionComplete`; `skipSet_noRest`; `skipExercise_marksPendingSkipped_andAdvances`; `goToSkippedExercise_reopensSkippedSets`; `pauseResume_excludesPausedTimeFromElapsed`; `pauseDuringRest_freezesRemaining`; `adjustRest_plus15_minusBelowZeroClamps`; `elapsed_wallClockJumpSameBoot_unaffected`; `elapsed_afterReboot_usesWall`; `checkFinish_noDoneSets_returnsNothingCompleted`; `editSet_changesActualValues`.
- [ ] Implementar; rodar; commit `feat(session): add pure workout session engine`.

### Tarefa 4.2: Persistência da sessão

**Arquivos:** `…/data/repository/SessionRepository.kt`, Teste: `…/data/repository/SessionRepositoryTest.kt`

**Produz:** `SessionRepository`: `observeActive(): Flow<ActiveSession?>`, `getActive(): ActiveSession?`, `start(routineId: Long, now: Mark, zone: ZoneId): Long` (falha com `IllegalStateException` se já existe ativa ou rotina sem exercícios; cria snapshots), `save(session: ActiveSession)` (atualiza sessão + todos os exercícios/séries numa transação), `finish(session, now, updateRoutineLoads: Boolean): Long` (status `FINISHED`, `ended`, `durationMs = elapsedMs`; atualiza cargas da rotina com a carga da última série `DONE` de cada exercício; chama `BackupTrigger`), `discard(sessionId)`.

- [ ] Testes: `start_snapshotsNamesAndTargets`; `start_withActiveSession_throws`; `start_emptyRoutine_throws`; `save_thenGetActive_roundTrips`; `routineDeletedDuringSession_sessionStillLoads`; `exerciseRenamedAfterFinish_historyKeepsOriginalName`; `finish_updatesRoutineLoadsWhenRequested`; `finish_storesDurationExcludingPauses`.
- [ ] Implementar; rodar; commit `feat(session): persist active session state at every step`.

### Tarefa 4.3: Controlador, alarme, notificações e service

**Arquivos:** `…/platform/session/{SessionController,RestAlarmScheduler,RestAlarmReceiver,SessionActionReceiver,SessionNotifications,WorkoutService,WorkoutServiceStarter}.kt`, `AndroidManifest.xml`, Testes: `SessionControllerTest.kt`, `RestAlarmSchedulerTest.kt`, `SessionNotificationsTest.kt`

**Produz:**
- `SessionController` (`@Singleton`, `Mutex` serializando operações): `val active: StateFlow<ActiveSession?>`; `suspend fun start(routineId): Long`, `completeSet(reps, loadKg)`, `skipSet()`, `skipExercise()`, `goToExercise(index)`, `editSet(...)`, `adjustRest(deltaSeconds)`, `setExerciseRest(index, seconds)`, `skipRest()`, `pause()`, `resume()`, `finish(updateRoutineLoads): Long`, `discard()`, `resync()`. Cada operação: engine → `SessionRepository.save` → reagenda/cancela alarme → atualiza notificação.
- `interface RestAlarmScheduler { fun schedule(triggerAtWallMs: Long); fun cancel() }`; implementação com `AlarmManager.setAlarmClock` (fallback `setAndAllowWhileIdle` se `!canScheduleExactAlarms()`).
- `RestAlarmReceiver`: posta "Descanso concluído" no canal `rest_end` (alta, som de notificação + vibração) ou `rest_end_alarm` (`USAGE_ALARM`), conforme `AppSettings.restSound`; `setTimeoutAfter(60_000)`.
- `SessionNotifications`: canal `session_ongoing` (baixa); notificação com `setUsesChronometer` (`setChronometerCountDown` durante descanso), texto fixo quando pausado, ações `ACTION_COMPLETE_SET` / `ACTION_SKIP_REST` → `SessionActionReceiver` (`goAsync` + controller); `setDeleteIntent` que repõe a notificação; `setOnlyAlertOnce`.
- `WorkoutService` (`foregroundServiceType="health"`): `ServiceCompat.startForeground(…, if (SDK ≥ 34) FOREGROUND_SERVICE_TYPE_HEALTH else 0)`; observa `active`; para quando `null`; `START_STICKY`; captura `ForegroundServiceStartNotAllowedException` → `stopSelf`.
- Manifest: `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_HEALTH`, `HIGH_SAMPLING_RATE_SENSORS`, `USE_EXACT_ALARM`, `SCHEDULE_EXACT_ALARM` (`maxSdkVersion="32"`), `VIBRATE`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.

- [ ] Testes (Robolectric shadows): `completeSet_schedulesAlarmAtRestEnd` (`ShadowAlarmManager.nextScheduledAlarm.triggerAtTime == wall + 90_000`); `skipRest_cancelsAlarm`; `pause_cancelsAlarm_resume_reschedulesRemaining`; `restAlarmReceiver_postsRestEndNotification`; `actionReceiver_completeSet_advancesSession`; `resync_withActiveSession_startsService_andReschedules`; `finish_stopsService_andCancelsAlarm`.
- [ ] Implementar; rodar; commit `feat(session): add foreground service, rest alarm and notification actions`.

### Tarefa 4.4: Tela de execução e resumo

**Arquivos:** `…/ui/feature/workout/{WorkoutScreen,WorkoutViewModel,WorkoutUiState,SessionSummaryScreen,SessionSummaryViewModel}.kt`, Testes: `WorkoutViewModelTest.kt`, `WorkoutFlowTest.kt`, `WorkoutScreenshotTest.kt`

**Produz:** `WorkoutUiState(routineName, elapsedText, isPaused, exerciseName, imagePath, setLabel /* "Série 2 de 3" */, repsValue, loadValue, restRemainingText: String?, restProgress: Float?, exercises: List<ExerciseChip>, nextUp: String?, canFinish)`; ticker de 250 ms só enquanto a tela está visível (`repeatOnLifecycle`), derivando do `active` + `TimeSource`.

Tela (imersiva, tema escuro forçado, `FLAG_KEEP_SCREEN_ON` se `keepScreenOn`):
- Topo: nome do treino, tempo total (Geist Mono), pausar/retomar, menu (pular exercício, ajustar descanso do exercício, encerrar, descartar).
- Centro: exercício, imagem (se houver), "Série X de Y", `NumberStepper` de reps e de carga (passos 1 e 2,5 kg), botão "Concluir série" (1 toque) e "Pular série".
- Descanso: `RestRing` com restante, `−15 s`/`+15 s`, "Pular descanso", próxima série/exercício; ao zerar no app aberto: pulso de cor `accent` + vibração (+ som próprio se notificações negadas).
- Faixa inferior com os exercícios (feito/atual/pendente/pulado), toque → `goToExercise`.
- Pede `POST_NOTIFICATIONS` no primeiro início de treino.
- Encerrar: se `NothingCompleted` → diálogo "Nenhuma série concluída. Descartar treino?"; senão `SessionSummaryRoute`, com resumo (duração, séries, volume total), checkbox "Atualizar cargas do treino" (padrão de `updateRoutineLoadsOnFinish`) e "Concluir".

- [ ] Teste VM: `restCountdown_derivesFromMarks_notTicks` (avançar `FakeTimeSource` 30 s sem tick ⇒ texto "1:00"); `finishWithoutCompletedSets_emitsDiscardPrompt`.
- [ ] `WorkoutFlowTest` (Compose + Robolectric, rotina com 2 exercícios × 2 séries): concluir → descanso visível → pular descanso → … → encerrar → resumo mostra 4 séries.
- [ ] Ligar `onStart(id)` de 3.1 a `controller.start` + navegação para `WorkoutRoute`.
- [ ] Screenshots: série, descanso, pausado, resumo; commit `feat(workout): add immersive workout screen and session summary`.

### Tarefa 4.5: Recuperação de sessão

**Arquivos:** `…/ui/navigation/ActiveSessionBar.kt`, `…/MainActivity.kt`, `…/MyFitApp.kt`, Teste: `SessionRecoveryTest.kt`

**Produz:** `ActiveSessionBar(session, elapsedText, onResume)` exibida acima da navegação em todas as abas quando há sessão ativa; `MyFitApp.onCreate` e `MainActivity.onStart` chamam `controller.resync()`.

- [ ] Teste: banco com sessão `ACTIVE` (descanso já vencido) → abrir `MainActivity` → barra "Treino em andamento" visível; tocar → `WorkoutRoute` no mesmo exercício/série, sem descanso.
- [ ] Implementar; commit `feat(workout): resume in-progress session after app restart`.
- [ ] Fim da etapa: verificação padrão de etapa + push.

---

## Etapa 5 — Histórico

### Tarefa 5.1: Cálculos de histórico

**Arquivos:** `…/domain/history/HistoryCalculator.kt`, Teste: `HistoryCalculatorTest.kt`

**Produz:** `SessionSummary(id, routineName, startDate: LocalDate, startTime: LocalTime, durationMs, setsDone, exercises: List<String>)`; `PeriodSummary(sessions: Int, totalDurationMs: Long, avgPerWeek: Double, exercises: List<ExerciseFrequency(name, sessions)>)`; `object HistoryCalculator { fun summarize(items: List<SessionSummary>, range: ClosedRange<LocalDate>): PeriodSummary; fun daysWithSessions(items): Set<LocalDate>; fun weekOf(date): ClosedRange<LocalDate> /* seg–dom */; fun monthOf(yearMonth): ClosedRange<LocalDate> }`. `avgPerWeek = sessions / (dias do intervalo / 7.0)`.

- [ ] Testes: `sessionCrossingMidnight_countsOnStartDate`; `summarize_week_countsAndTotals`; `avgPerWeek_forMonth_of30Days`; `exercises_sortedByFrequencyThenName`; `weekOf_sunday_returnsPreviousMonday`.
- [ ] Implementar; commit `feat(history): add weekly and monthly summary calculations`.

### Tarefa 5.2: Consultas de histórico

**Arquivos:** `…/data/db/dao/SessionDao.kt`, `…/data/repository/HistoryRepository.kt`, Teste: `HistoryRepositoryTest.kt`

**Produz:** `HistoryRepository`: `observeSummaries(range: ClosedRange<LocalDate>): Flow<List<SessionSummary>>` (só `FINISHED`), `observeDetail(sessionId): Flow<SessionDetail?>` (`SessionDetail(summary, exercises: List<SessionExerciseDetail(name, status, sets: List<SessionSet>)>)`), `deleteSession(id)`.

- [ ] Testes: `summaries_excludeActive`; `summaries_filteredByRange`; `detail_returnsSetsInOrder`.
- [ ] Implementar; commit `feat(history): add history queries`.

### Tarefa 5.3: Telas de histórico e detalhe

**Arquivos:** `…/ui/feature/history/{HistoryScreen,HistoryViewModel,MonthCalendar,SessionDetailScreen,SessionDetailViewModel}.kt`, Testes: `HistoryViewModelTest.kt`, `HistoryScreenshotTest.kt`

**Produz:** `MonthCalendar(month: YearMonth, marked: Set<LocalDate>, selected: LocalDate?, today: LocalDate, onSelect, onMonthChange)` (grade seg–dom, swipe entre meses, dia treinado = ponto/realce `accent`); resumo com alternância Semana/Mês (treinos, tempo total, frequência semanal, exercícios); lista do dia selecionado → `SessionDetailRoute`; detalhe com exercícios, séries (reps × carga, puladas esmaecidas) e excluir sessão.

- [ ] Teste VM: `selectingDay_listsThatDaysSessions`; `toggleMonth_switchesSummaryRange`.
- [ ] Screenshots: mês com treinos, dia selecionado, detalhe; commit `feat(history): add calendar, period summaries and session detail`.
- [ ] Fim da etapa: verificação padrão de etapa + push.

---

## Etapa 6 — Início e MVP

### Tarefa 6.1: Dashboard

**Arquivos:** `…/ui/feature/home/{HomeScreen,HomeViewModel}.kt`, Testes: `HomeViewModelTest.kt`, `HomeScreenshotTest.kt`

**Produz:** `HomeUiState(greeting /* Bom dia/Boa tarde/Boa noite */, dateText /* "quinta-feira, 12 de novembro" */, active: ActiveSession?, today: List<SessionSummary>, suggested: RoutineOverview?, routines: List<RoutineOverview>, last: SessionSummary?, body: BodySnapshot?, week: WeekStrip(days: List<DayMark>, sessions, totalDurationMs))`. `BodySnapshot` vem de 7.1 (até lá, `null`). Sugestão: rotina com exercícios nunca feita (menor posição) senão a de `lastDoneEpochDay` mais antigo.

- [ ] Testes: `greeting_byHour` (5–11 dia, 12–17 tarde, resto noite); `suggestion_prefersNeverDone_thenOldest`; `today_listsOnlyTodaysFinishedSessions`.
- [ ] Tela na ordem: saudação/data → sessão ativa (Retomar) ou treino de hoje → "Iniciar" com sugerido → treinos (carrossel) → última atividade → corpo → semana (7 pontos seg–dom). Screenshots vazio e com dados.
- [ ] Commit `feat(home): add dashboard`.

### Tarefa 6.2: Release assinado e MVP v0.1.0

**Arquivos:** `app/build.gradle.kts`, `app/proguard-rules.pro`, `README.md`

- [ ] `keytool -genkeypair -v -keystore ~/my/.keystores/myfit-release.jks -alias myfit -keyalg RSA -keysize 4096 -validity 36500` com senha aleatória; gravar `MYFIT_STORE_FILE`, `MYFIT_STORE_PASSWORD`, `MYFIT_KEY_ALIAS`, `MYFIT_KEY_PASSWORD` em `~/.gradle/gradle.properties` (fora do repo).
- [ ] `signingConfigs.release` lido dessas propriedades; se ausentes, release sai sem assinatura (build não quebra).
- [ ] Regras R8 para kotlinx.serialization (DTOs/rotas), Hilt/Room (geradas), Tink.
- [ ] `versionCode = 1`, `versionName = "0.1.0"`; `./gradlew assembleRelease` sem avisos de classe ausente; `apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk` → assinado; tamanho < 15 MB.
- [ ] README: seção "Instalação" (fluxo para apps de desenvolvedor não verificado / ADB) e "Status do MVP".
- [ ] Commit `build: configure release signing and r8`; tag `v0.1.0`; `gh release create v0.1.0 --prerelease` com o APK `myfit-0.1.0.apk`; copiar para `/mnt/c/Users/luann.silva/Downloads/`.
- [ ] Fim da etapa: verificação padrão de etapa + push (com tags).

---

## Etapa 7 — Evolução

### Tarefa 7.1: Domínio corporal

**Arquivos:** `…/domain/body/{Measure,BodyComparison,BodySeries}.kt`, `…/domain/model/BodyAssessment.kt`, Teste: `BodyComparisonTest.kt`

**Produz:** `BodyAssessment(id, date: LocalDate, values: Map<Measure, Double>, notes)`; `enum class Measure(labelRes, unit: MeasureUnit /* KG, PERCENT, CM */)` na ordem WEIGHT, BODY_FAT, NECK, SHOULDERS, CHEST, WAIST, ABDOMEN, HIPS, ARM_RIGHT, ARM_LEFT, FOREARM_RIGHT, FOREARM_LEFT, THIGH_RIGHT, THIGH_LEFT, CALF_RIGHT, CALF_LEFT; `MeasureDiff(measure, from: Double?, to: Double?, diff: Double?)`; `object BodyComparison { fun compare(from: BodyAssessment, to: BodyAssessment): List<MeasureDiff>; fun series(items: List<BodyAssessment>, measure: Measure): List<Pair<LocalDate, Double>> }`; `BodySnapshot(latest: BodyAssessment, weightDelta: Double?, highlights: List<MeasureDiff>)` para o Início.

- [ ] Testes: `compare_diffIsToMinusFrom` (peso 71→73 = +2; cintura 78→79 = +1); `compare_missingValue_diffNull`; `series_sortedByDate_skipsMissing`; `diffFormat_signed` ("+2 kg", "−1,5 cm", "0 cm").
- [ ] Implementar; commit `feat(body): add measures, comparison and series`.

### Tarefa 7.2: Avaliações (lista e editor)

**Arquivos:** `…/data/repository/BodyRepository.kt`, `…/ui/feature/evolution/{EvolutionScreen,EvolutionViewModel,AssessmentEditorScreen,AssessmentEditorViewModel}.kt`, Testes: `BodyRepositoryTest.kt`, `AssessmentEditorViewModelTest.kt`

**Produz:** `BodyRepository`: `observeAssessments(): Flow<List<BodyAssessment>>`, `get(id)`, `save(a): Long`, `delete(id)`, `observePhotos()`, `addPhoto(date, pose, relativePath)`, `deletePhoto(id)` (todas as escritas chamam `BackupTrigger`). Aba Evolução com seções: Medidas (lista com data e peso), Gráficos, Comparar, Fotos. Editor com data (DatePicker), campos agrupados (Gerais, Tronco, Braços, Pernas) usando `DecimalInput`, pré-preenchidos como placeholder com a última avaliação.

- [ ] Testes: `save_thenObserve_newestFirst`; `editor_requiresDate_only`; `editor_rejectsInvalidDecimal_keepsPrevious`; `home_bodySnapshot_usesLatestTwo`.
- [ ] Ligar `BodySnapshot` ao Início; screenshots; commit `feat(evolution): add body assessments list and editor`.

### Tarefa 7.3: Comparação e gráficos

**Arquivos:** `…/ui/feature/evolution/{AssessmentCompareScreen,CompareViewModel,ChartsSection,ChartsViewModel}.kt`, Testes: `CompareViewModelTest.kt`, `ChartsViewModelTest.kt`, `EvolutionScreenshotTest.kt`

**Produz:** comparação com seletores "De"/"Para" (padrão: primeira e última), tabela Indicador | De | Para | Diferença (diferença com sinal em cor neutra, sem julgar se subir ou descer é bom); gráficos Vico `LineCartesianLayer` por indicador, seleção por `FilterChipRow` (padrão: Peso, Cintura, Braço D), período Tudo/1 ano/6 meses; um gráfico por indicador selecionado (unidades diferentes).

- [ ] Testes: `compare_defaultsToFirstAndLast`; `charts_selectionKeptInViewModel`; `charts_periodFilter_trimsSeries`.
- [ ] Screenshots: comparação e gráficos com 6 avaliações; commit `feat(evolution): add assessment comparison and measure charts`.

### Tarefa 7.4: Fotos corporais

**Arquivos:** `…/ui/feature/evolution/{PhotosScreen,PhotosViewModel,PhotoCompareScreen}.kt`, Teste: `PhotosViewModelTest.kt`

**Produz:** grade agrupada por data (mais recente primeiro), adicionar (data + pose + `PickVisualMedia` → `ImageStore.import(…, BODY)`), excluir (apaga arquivo), comparar duas fotos lado a lado (seleção de 2, mesma pose sugerida) com datas.

- [ ] Testes: `photos_groupedByDateDesc`; `deletePhoto_removesFile`; `compare_requiresExactlyTwo`.
- [ ] Screenshots; commit `feat(evolution): add body photos and side-by-side comparison`.
- [ ] Fim da etapa: verificação padrão de etapa + push.

---

## Etapa 8 — Formato de backup

### Tarefa 8.1: Snapshot dos dados (DTOs v1)

**Arquivos:** `…/domain/backup/dto/BackupDataV1.kt`, `…/data/backup/SnapshotMapper.kt`, Teste: `SnapshotMapperTest.kt`

**Produz:** `@Serializable data class BackupDataV1(exercises, routines, routineExercises, sessions, sessionExercises, sessionSets, assessments, photos: List<…Dto>)` — DTOs espelham as entidades com campos primitivos e nomes estáveis; `SnapshotMapper`: `suspend fun export(db): BackupDataV1`, `suspend fun replaceAll(db, data)` (uma `withTransaction`: apaga na ordem filho→pai e insere pai→filho preservando IDs), `fun referencedImages(data): Set<String>`, `fun counts(data): Map<String, Int>`.

- [ ] Testes: `exportReplaceAll_roundTripsEveryTable` (popular todas as tabelas, exportar, limpar, reimportar, comparar listas); `json_isStable` (serializa → desserializa igual; campos desconhecidos ignorados).
- [ ] Commit `feat(backup): add versioned data snapshot`.

### Tarefa 8.2: Criptografia e cabeçalho

**Arquivos:** `…/domain/backup/{BackupHeader,BackupCrypto,KeyDerivation}.kt`, Teste: `BackupCryptoTest.kt`

**Produz:**
- Cabeçalho binário: `"MYFITBAK"` (8 B) | `headerVersion: u16 = 1` | `kdf: u8 = 1 (PBKDF2-HMAC-SHA256)` | `iterations: u32` | `salt: 16 B` | `kcv: 16 B` | `createdAtMs: u64`. `BackupHeader.read(input): BackupHeader` / `write(out)` / `bytes()`.
- `object KeyDerivation { fun derive(password: CharArray, salt: ByteArray, iterations: Int = 600_000): ByteArray /* 32 B, NFKC */; fun kcv(key): ByteArray /* HKDF-SHA256(key, info="myfit-kcv"), 16 B */ }`.
- `object BackupCrypto { fun encrypt(key, header, plaintext: InputStream, out: OutputStream); fun decrypt(key, input: InputStream, out: OutputStream): BackupHeader }` — Tink `AesGcmHkdfStreaming` (AES-256, HKDF-SHA256, segmento 1 MiB), `associatedData = header.bytes()`. Erros: `BackupError.WrongPassword` (kcv ≠), `BackupError.Corrupted(reason)`, `BackupError.UnsupportedVersion(found)` (`sealed class BackupError : Exception`).

- [ ] Testes (iterações baixas via parâmetro): `roundTrip_3MB`; `wrongPassword_detectedByKcv`; `flippedCiphertextByte_corrupted`; `tamperedHeaderCreatedAt_corrupted`; `truncatedFile_corrupted`; `badMagic_corrupted`; `headerVersion2_unsupported`.
- [ ] Commit `feat(backup): add authenticated streaming encryption with password-derived key`.

### Tarefa 8.3: Arquivo zip e manifest

**Arquivos:** `…/domain/backup/{BackupArchive,BackupManifest}.kt`, Teste: `BackupArchiveTest.kt`

**Produz:** `@Serializable BackupManifest(formatVersion = 1, appVersionName, appVersionCode, createdAtMs, dbSchemaVersion, counts: Map<String, Int>, entries: List<ManifestEntry(path, size, sha256)>)`; `object BackupArchive { fun write(out, data: BackupDataV1, settingsJson: String, images: Map<String, File>, meta: ManifestMeta); fun read(input, stagingDir: File, limits: ArchiveLimits = ArchiveLimits()): ArchiveContents(manifest, data, settingsJson, imageDir) }` — `data.json`/`settings.json` DEFLATED, imagens STORED; leitura valida hashes, nomes (`..`, absolutos, fora de `images/`), máximo 20 000 entradas e 2 GiB descomprimidos, `formatVersion ≤ 1`.

- [ ] Testes: `writeRead_roundTrip_withImages`; `hashMismatch_rejected`; `zipSlipEntry_rejected`; `missingManifest_rejected`; `newerFormatVersion_rejected`; `entryNotInManifest_rejected`.
- [ ] Commit `feat(backup): add backup archive with manifest and integrity checks`.

### Tarefa 8.4: Senha de recuperação e chave guardada

**Arquivos:** `…/data/backup/{BackupKeyStore,KeyWrapper,AndroidKeystoreKeyWrapper}.kt`, `…/ui/feature/settings/BackupPasswordDialog.kt`, Teste: `BackupKeyStoreTest.kt`

**Produz:** `interface KeyWrapper { fun wrap(plain: ByteArray): ByteArray; fun unwrap(wrapped: ByteArray): ByteArray }` (Keystore AES-GCM, alias `myfit_backup_wrap`, sem auth, sem `setUnlockedDeviceRequired`, IV prefixado); `BackupKeyStore(dataStore/arquivo, wrapper, timeSource)`: `suspend fun setPassword(password: CharArray)` (gera salt, deriva, guarda `salt`, `iterations`, `kcv`, chave embrulhada), `suspend fun key(): StoredKey?` (`null` se não definida ou unwrap falhar → status "senha necessária"), `suspend fun unlock(password): Boolean` (re-deriva com salt salvo e confere kcv), `hasPassword(): Flow<Boolean>`. Diálogo: senha + confirmação, mínimo 8 caracteres, aviso "sem a senha não há como restaurar".

- [ ] Testes (com `FakeKeyWrapper`): `setPassword_thenKey_returnsSameKey`; `unwrapFailure_returnsNull_andUnlockRestores`; `unlock_wrongPassword_false`.
- [ ] Commit `feat(backup): store password-derived key wrapped by android keystore`.

### Tarefa 8.5: Orquestração de criar e restaurar

**Arquivos:** `…/data/backup/BackupManager.kt`, Teste: `BackupManagerTest.kt`

**Produz:** `BackupManager`: `suspend fun create(out: OutputStream): BackupInfo` (snapshot → zip temporário em `cacheDir` → criptografa para `out`; exige chave), `suspend fun inspect(input: InputStream): BackupHeader` (lê só o cabeçalho → data para confirmação), `suspend fun restore(input: InputStream, password: CharArray?): RestoreResult`, `suspend fun recoverInterruptedRestore()`. `sealed interface RestoreResult { Success(counts); WrongPassword; Corrupted(reason); UnsupportedVersion; InsufficientSpace; SessionActive }`.

Restore: sessão ativa ⇒ `SessionActive`; espaço livre < 2× tamanho ⇒ `InsufficientSpace`; decripta para `cacheDir` → `BackupArchive.read` em `filesDir/restore_staging` → cancela workers de backup → grava marcador `filesDir/restore.pending` → `SnapshotMapper.replaceAll` → `images/` vira `images_old`, staging vira `images` → importa settings → remove `images_old` e marcador → valida contagens e que toda referência de imagem existe → `resync` de alarmes. `recoverInterruptedRestore` (chamado no `MyFitApp.onCreate`): com marcador, conclui a troca de diretórios.

- [ ] Testes (Robolectric, banco + arquivos reais): `createRestore_roundTrip_restoresDbImagesAndSettings`; `restore_wrongPassword_leavesLocalDataUntouched`; `restore_corrupted_leavesLocalDataUntouched`; `restore_withActiveSession_refused`; `recoverInterruptedRestore_completesSwap`.
- [ ] Commit `feat(backup): add backup creation and atomic restore`.

### Tarefa 8.6: Backup manual e restauração por arquivo (UI)

**Arquivos:** `…/ui/feature/settings/{BackupScreen,BackupViewModel}.kt`, Teste: `BackupViewModelTest.kt`

**Produz:** tela Backup com: senha (definir/alterar), "Exportar backup" (`CreateDocument("application/octet-stream")`, nome `myfit-backup-AAAAMMDD-HHmmss.myfit`), "Restaurar de arquivo" (`OpenDocument`) → mostra data do backup (`inspect`) → `ConfirmDialog` destrutivo → pede senha se `key()` nulo ou se o salt do arquivo difere → resultado em mensagem clara por caso de `RestoreResult`.

- [ ] Testes VM: `export_withoutPassword_promptsPasswordFirst`; `restore_showsBackupDateBeforeConfirm`; `restoreResult_wrongPassword_message`.
- [ ] Commit `feat(backup): add manual export and restore from file`.
- [ ] Fim da etapa: verificação padrão de etapa + push.

---

## Etapa 9 — Backup automático na pasta

### Tarefa 9.1: Destino em pasta e retenção

**Arquivos:** `…/data/backup/{BackupDestination,SafBackupDestination,FileBackupDestination}.kt`, `…/domain/backup/Retention.kt`, Testes: `RetentionTest.kt`, `FolderBackupWriterTest.kt`

**Produz:**
- `interface BackupDestination { suspend fun list(): List<BackupFileInfo>; suspend fun write(name: String, block: suspend (OutputStream) -> Unit): BackupFileInfo; suspend fun open(file: BackupFileInfo): InputStream; suspend fun delete(file: BackupFileInfo) }`; `BackupFileInfo(id: String, name: String, sizeBytes: Long, createdAtMs: Long)` (data extraída do nome).
- `SafBackupDestination(context, treeUri)` com `DocumentFile`: escreve `<nome>.partial`, renomeia (fallback: grava direto no nome final se `renameTo` falhar), lança `BackupError.FolderUnavailable` em `SecurityException`/pasta inexistente. `FileBackupDestination(dir)` para testes.
- `object Retention { val NAME_REGEX = Regex("""myfit-backup-(\d{8})-(\d{6})\.myfit"""); fun toDelete(files: List<BackupFileInfo>, keep: Int = 7): List<BackupFileInfo> }` — ignora arquivos fora do padrão.
- `FolderBackupWriter(manager, destination)`: `suspend fun run(): BackupFileInfo` → escreve, relê o cabeçalho e compara tamanho, depois apaga os excedentes.

- [ ] Testes: `toDelete_keepsNewest7`; `toDelete_ignoresForeignFiles`; `run_writesValidatesThenPrunes` (9 existentes → 7 após); `run_validationFails_doesNotPrune`.
- [ ] Commit `feat(backup): write backups to user folder with retention`.

### Tarefa 9.2: Agendamento

**Arquivos:** `…/platform/backup/{BackupScheduler,BackupWorker,BackupDebounceWorker}.kt`, `…/data/prefs/BackupStateStore.kt`, Testes: `BackupWorkerTest.kt`, `BackupSchedulerTest.kt`

**Produz:** `BackupState(folderUri: String?, folderName: String?, dirtySinceMs: Long?, lastSuccessMs: Long?, lastFileName: String?, lastAttemptMs: Long?, lastError: BackupFailure?)`, `enum BackupFailure { NO_FOLDER, FOLDER_UNAVAILABLE, NO_PASSWORD, NO_SPACE, UNKNOWN }`; `BackupScheduler : BackupTrigger`: `onDataChanged()` (marca dirty + `enqueueUniqueWork("backup-debounce", REPLACE, delay 10 min)`), `ensurePeriodic()` (`"backup-daily"`, 24 h, `KEEP`, `requiresBatteryNotLow`, `requiresStorageNotLow`), `runNow()` (`"backup-run"` expedited com `force = true`). `BackupDebounceWorker` só enfileira `"backup-run"` com `KEEP`. `BackupWorker(force)`: se `!force && dirtySinceMs == null` ⇒ `success` sem gravar; se `autoBackup` off e `!force` ⇒ sem gravar; sucesso limpa dirty (só se `dirtySinceMs` não mudou durante a execução) e grava `lastSuccessMs`; falha grava `lastError`, mantém dirty, notifica (canal `backup_status`) e retorna `failure` (sem retry infinito para `FOLDER_UNAVAILABLE`/`NO_PASSWORD`; `retry` com backoff para `UNKNOWN`).

- [ ] Testes (`TestListenableWorkerBuilder` + `FileBackupDestination`): `clean_notForced_skips`; `dirty_writes_andClearsDirty`; `changeDuringRun_keepsDirty`; `folderUnavailable_setsError_keepsDirty_noRetry`; `noPassword_setsError`; `onDataChanged_enqueuesDebounce`.
- [ ] Ligar `BackupTrigger` real no Hilt; `ensurePeriodic()` no `MyFitApp.onCreate`; commit `feat(backup): schedule daily and change-triggered backups`.

### Tarefa 9.3: Tela de backup automático

**Arquivos:** `…/ui/feature/settings/{BackupScreen,BackupViewModel}.kt`, Teste: `BackupScreenshotTest.kt`

**Produz:** seção "Pasta de backup" (escolher/trocar via `OpenDocumentTree` + `takePersistableUriPermission` read/write; nome da pasta), status (último backup "hoje às 14:32", último erro em `danger` com ação corretiva), toggle automático, "Fazer backup agora" (progresso via `WorkInfo`), lista dos backups da pasta (data, tamanho) → fluxo de restauração de 8.6, dica "use um app de sincronização para manter esta pasta no Drive".

- [ ] Screenshots: sem pasta, OK, com erro; commit `feat(backup): add automatic folder backup settings and restore list`.
- [ ] Fim da etapa: verificação padrão de etapa + push.

---

## Etapa 10 — Polimento

### Tarefa 10.1: Ajustes

**Arquivos:** `…/ui/feature/settings/{SettingsScreen,SettingsViewModel}.kt`, Teste: `SettingsViewModelTest.kt`

**Produz:** seções Aparência (tema), Treino (descanso padrão, som do descanso, manter tela ligada, atualizar cargas ao finalizar), Dados (Backup →, Biblioteca de exercícios →), Bateria (mostra se otimização está ativa; abre `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`; texto sobre "Sem restrições" na Samsung), Sobre (versão, licenças de fontes).

- [ ] Testes: `themeChange_persists`; `defaultRest_clampedTo15to600`.
- [ ] Commit `feat(settings): add preferences screen`.

### Tarefa 10.2: Revisão de qualidade

- [ ] Estados vazios e de carregamento em todas as telas; animações (`animateContentSize`, fade 200 ms nas transições de rota, descanso com `animateFloatAsState`).
- [ ] Screenshots de todas as telas em: escuro, claro, 840 dp (rail), fonte 1.5×; corrigir cortes e contraste.
- [ ] Acessibilidade: `contentDescription` em ícones de ação, alvos ≥ 48 dp, cronômetros com `semantics { liveRegion }` só no fim do descanso.
- [ ] Limpeza de imagens órfãs agendada semanalmente (worker leve) usando `ImageStore.deleteOrphans`.
- [ ] `./gradlew lintDebug` sem erros e sem avisos novos relevantes; `assembleRelease` limpo.
- [ ] Commits `fix(ui): …` / `refactor: …` por assunto; push.

---

## Etapa 11 — Documentação e release

### Tarefa 11.1: Documentação

**Arquivos:** `README.md`, `docs/ARCHITECTURE.md`, `docs/TESTING.md`, `docs/BRAND.md`, `docs/screenshots/*.png`

- [ ] README: o que é, screenshots, recursos, stack, instalação do APK (incluindo verificação de desenvolvedor no Brasil, conferida na data), backup na pasta + sincronização com o Drive, desenvolvimento (toolchain, comandos Gradle, testes, screenshots), estrutura, licença das fontes.
- [ ] ARCHITECTURE: camadas, esquema do banco, motor da sessão e modelo de tempo, plataforma (service/alarme/notificações), formato do backup (cabeçalho, zip, criptografia, restauração atômica), decisões e trade-offs.
- [ ] TESTING: roteiro manual no celular cobrindo os critérios de conclusão do SPEC (instalação, treino com tela bloqueada, matar app e retomar, reiniciar celular no meio, calendário, medidas/gráficos, backup na pasta, restauração em instalação limpa, perda de permissão da pasta, Samsung "Sem restrições").
- [ ] Commit `docs: add architecture, testing guide and final readme`.

### Tarefa 11.2: Release v1.0.0

- [ ] `versionCode = 10`, `versionName = "1.0.0"`; `./gradlew lintDebug testDebugUnitTest assembleRelease`; `apksigner verify`; tamanho < 15 MB.
- [ ] Commit `chore: release 1.0.0`; tag `v1.0.0`; `gh release create v1.0.0` com `myfit-1.0.0.apk` e notas em PT-BR; copiar para `/mnt/c/Users/luann.silva/Downloads/`; push com tags.
