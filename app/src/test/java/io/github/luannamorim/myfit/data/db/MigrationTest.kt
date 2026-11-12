package io.github.luannamorim.myfit.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.data.db.seed.ExerciseSeed
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

/** Opens databases created by older releases (schema versions 1 and 2) with the current app. */
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
        .addMigrations(*ExerciseSeed.MIGRATIONS)
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
        assertEquals(3, room.openHelper.readableDatabase.version)
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
    fun freshInstall_getsWholeLibrary_once_withKeys() = runBlocking {
        context.deleteDatabase("fresh-test.db")
        val room = open("fresh-test.db")
        val all = room.exerciseDao().getAll()
        assertEquals(ExerciseSeed.items.size, all.size)
        assertEquals(ExerciseSeed.items.map { it.key }.toSet(), all.mapNotNull { it.seedKey }.toSet())
        room.close()
    }
}
