package io.github.luannamorim.myfit.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.data.db.entity.ActivityEntity
import io.github.luannamorim.myfit.data.db.seed.ExerciseSeed
import io.github.luannamorim.myfit.domain.activity.Sport
import io.github.luannamorim.myfit.domain.model.ExerciseMeasure
import io.github.luannamorim.myfit.domain.model.Intensity
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

/** Opens databases created by older releases (schema versions 1, 2 and 3) with the current app. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class MigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    /** Builds a real database file from an exported schema, like an installed older app. */
    private fun createVersion(version: Int, name: String, prepare: (SQLiteDatabase) -> Unit) {
        val schema = Json.parseToJsonElement(File("schemas/io.github.luannamorim.myfit.data.db.MyFitDatabase/$version.json").readText()).jsonObject
        val database = schema.getValue("database").jsonObject
        val file = context.getDatabasePath(name).apply { parentFile?.mkdirs(); delete() }
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            database.getValue("entities").jsonArray.forEach { e ->
                val entity = e.jsonObject
                val table = entity.getValue("tableName").jsonPrimitive.content
                db.execSQL(entity.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", table))
                entity["indices"]?.jsonArray?.forEach { i ->
                    db.execSQL(i.jsonObject.getValue("createSql").jsonPrimitive.content.replace("\${TABLE_NAME}", table))
                }
            }
            database.getValue("setupQueries").jsonArray.forEach { db.execSQL(it.jsonPrimitive.content) }
            prepare(db)
            db.version = version
        }
    }

    private fun open(name: String) = Room.databaseBuilder(context, MyFitDatabase::class.java, name)
        .addCallback(ExerciseSeed.Callback())
        .addMigrations(*Migrations.ALL)
        .allowMainThreadQueries()
        .build()

    private fun SQLiteDatabase.insertExercise(name: String, custom: Boolean) = insert(
        "exercise",
        null,
        ContentValues().apply {
            put("name", name); put("muscleGroup", "QUADRICEPS"); put("movementType", "LEGS")
            put("isCustom", if (custom) 1 else 0); put("archived", 0); put("createdAtMs", 0)
        },
    )

    @Test
    fun migrate1To3_addsNewLibraryExercises_withoutDuplicatesOrTouchingUserData() = runBlocking {
        createVersion(1, "migration-test.db") { db ->
            ExerciseSeed.items.filter { it.since == 1 }.forEach { db.insertExercise(it.namePt, custom = false) }
            db.insertExercise("Goblet Squat", custom = true) // the user already created this one, in English
        }

        val room = open("migration-test.db")
        val all = room.exerciseDao().getAll()
        val names = all.map { it.name }

        // Every library entry exists exactly once; the user's "Goblet Squat" stands in for ours.
        assertEquals(ExerciseSeed.items.size, names.size)
        assertEquals(1, names.count { it.equals("Goblet squat", ignoreCase = true) || it == "Agachamento goblet" })
        assertTrue("Prancha lateral" in names) // device language (pt-BR in tests)
        assertTrue("Agachamento no Smith" in names)
        val goblet = all.single { it.name == "Goblet Squat" }
        assertTrue(goblet.isCustom)
        assertEquals("goblet-squat", goblet.seedKey)
        all.filter { !it.isCustom }.forEach { e -> assertEquals(e.name, ExerciseSeed.forKey(e.seedKey)?.namePt) }
        assertEquals(MyFitDatabase.VERSION, room.openHelper.readableDatabase.version)
        room.close()
    }

    @Test
    fun migrate2To3_linksLibraryRows_keepsRenamedAndCustomOnes_andFillsSessionSnapshots() = runBlocking {
        var benchId = 0L
        createVersion(2, "migration-v2-test.db") { db ->
            ExerciseSeed.items.filter { it.since <= 2 }.forEach { item ->
                val id = db.insertExercise(if (item.key == "lateral-raise") "Minha elevação" else item.namePt, custom = false)
                if (item.key == "barbell-bench-press") benchId = id
            }
            db.insertExercise("Supino reto no Smith", custom = true) // same name as a version-3 entry
            val sessionId = db.insert(
                "workout_session",
                null,
                ContentValues().apply {
                    put("routineName", "Peito"); put("status", "FINISHED"); put("totalPausedMs", 0)
                    put("cursorExercise", 0); put("cursorSet", 0); put("startDateEpochDay", 20_000)
                    put("started_wallMs", 0); put("started_elapsedMs", 0); put("started_bootCount", 0)
                },
            )
            db.insert(
                "session_exercise",
                null,
                ContentValues().apply {
                    put("sessionId", sessionId); put("exerciseId", benchId); put("position", 0)
                    put("name", "Supino reto com barra"); put("muscleGroup", "CHEST"); put("movementType", "PUSH")
                    put("restSeconds", 90); put("status", "DONE")
                },
            )
        }

        val room = open("migration-v2-test.db")
        val all = room.exerciseDao().getAll()

        // v2 rows (one renamed) + the user's "Supino reto no Smith" + version-3 entries minus that one.
        assertEquals(ExerciseSeed.items.size, all.size)
        assertEquals("barbell-bench-press", all.single { it.id == benchId }.seedKey)
        assertEquals(null, all.single { it.name == "Minha elevação" }.seedKey) // renamed: no longer recognisable
        assertTrue(all.none { it.name == "Elevação lateral" }) // older entries the user changed are not re-added
        val smith = all.filter { it.name == "Supino reto no Smith" }
        assertEquals(1, smith.size)
        assertTrue(smith.single().isCustom)
        assertEquals("smith-machine-bench-press", smith.single().seedKey)
        room.openHelper.readableDatabase.query("SELECT seedKey FROM session_exercise").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("barbell-bench-press", c.getString(0))
        }
        room.close()
    }

    @Test
    fun migrate3To4_keepsSessions_andAddsIntensityPhotoAndActivities() = runBlocking {
        createVersion(3, "migration-v3-test.db") { db ->
            ExerciseSeed.items.forEach { db.insertExercise(it.namePt, custom = false) }
            db.insert(
                "workout_session",
                null,
                ContentValues().apply {
                    put("routineName", "Costas"); put("status", "FINISHED"); put("totalPausedMs", 0); put("durationMs", 3_600_000)
                    put("cursorExercise", 0); put("cursorSet", 0); put("startDateEpochDay", 20_400)
                    put("started_wallMs", 0); put("started_elapsedMs", 0); put("started_bootCount", 0)
                },
            )
        }

        val room = open("migration-v3-test.db")
        val session = room.sessionDao().getAllSessions().single()
        assertEquals("Costas", session.routineName)
        assertEquals(3_600_000L, session.durationMs)
        assertEquals(null, session.intensity)
        assertEquals(null, session.imagePath)
        room.sessionDao().setIntensity(session.id, Intensity.HIGH.name)
        assertEquals(Intensity.HIGH, room.sessionDao().getAllSessions().single().intensity)

        val id = room.activityDao().insert(
            ActivityEntity(sport = Sport.SWIMMING, dateEpochDay = 20_401, durationMinutes = 45, distanceKm = 1.5, createdAtMs = 1),
        )
        assertEquals(Sport.SWIMMING, room.activityDao().get(id)!!.sport)
        assertEquals(MyFitDatabase.VERSION, room.openHelper.readableDatabase.version)
        room.close()
    }

    @Test
    fun migrate4To5_makesHoldsAndCardioTimed_withTimeTargetsWhereRepsWereSet() = runBlocking {
        var plankItem = 0L
        var treadmillItem = 0L
        var benchItem = 0L
        createVersion(4, "migration-v4-test.db") { db ->
            val ids = ExerciseSeed.items.associate { item ->
                item.key to db.insert(
                    "exercise",
                    null,
                    ContentValues().apply {
                        put("name", item.namePt); put("muscleGroup", item.muscleGroup.name); put("movementType", item.movementType.name)
                        put("isCustom", 0); put("archived", 0); put("createdAtMs", 0); put("seedKey", item.key)
                    },
                )
            }
            val routine = db.insert("routine", null, ContentValues().apply { put("name", "Core"); put("position", 0); put("createdAtMs", 0); put("updatedAtMs", 0) })
            fun item(exercise: Long, reps: Int, position: Int) = db.insert(
                "routine_exercise",
                null,
                ContentValues().apply {
                    put("routineId", routine); put("exerciseId", exercise); put("position", position)
                    put("sets", 3); put("reps", reps); put("restSeconds", 60)
                },
            )
            plankItem = item(ids.getValue("plank"), 12, 0) // reps-like: becomes 30 s
            treadmillItem = item(ids.getValue("treadmill"), 900, 1) // already seconds: kept
            benchItem = item(ids.getValue("barbell-bench-press"), 10, 2)
        }

        val room = open("migration-v4-test.db")
        val byKey = room.exerciseDao().getAll().associateBy { it.seedKey }
        assertEquals(ExerciseMeasure.TIME, byKey.getValue("plank").measure)
        assertEquals(ExerciseMeasure.TIME, byKey.getValue("side-plank").measure)
        assertEquals(ExerciseMeasure.TIME, byKey.getValue("treadmill").measure)
        assertEquals(ExerciseMeasure.REPS, byKey.getValue("barbell-bench-press").measure)
        val reps = room.routineDao().getAllItems().associate { it.id to it.reps }
        assertEquals(30, reps[plankItem])
        assertEquals(900, reps[treadmillItem])
        assertEquals(10, reps[benchItem])
        assertEquals(MyFitDatabase.VERSION, room.openHelper.readableDatabase.version)
        room.close()
    }

    @Test
    fun freshInstall_getsWholeLibrary_once_withKeys() = runBlocking {
        context.deleteDatabase("fresh-test.db")
        val room = open("fresh-test.db")
        val all = room.exerciseDao().getAll()
        assertEquals(ExerciseSeed.items.size, all.size)
        assertEquals(ExerciseSeed.items.map { it.key }.toSet(), all.mapNotNull { it.seedKey }.toSet())
        assertEquals(
            ExerciseSeed.items.filter { it.measure == ExerciseMeasure.TIME }.map { it.key }.toSet(),
            all.filter { it.measure == ExerciseMeasure.TIME }.mapNotNull { it.seedKey }.toSet(),
        )
        room.close()
    }
}
