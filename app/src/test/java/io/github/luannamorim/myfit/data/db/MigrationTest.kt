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

/** Opens a database created by v1.1.0 (schema version 1) with the current app. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class MigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    /** Builds a real version-1 database file from the exported schema, like an installed v1.x app. */
    private fun createVersion1(name: String, prepare: (SQLiteDatabase) -> Unit) {
        val schema = Json.parseToJsonElement(File("schemas/io.github.luannamorim.myfit.data.db.MyFitDatabase/1.json").readText()).jsonObject
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
            db.version = 1
        }
    }

    private fun SQLiteDatabase.insertExercise(name: String, custom: Boolean) = insert(
        "exercise",
        null,
        ContentValues().apply {
            put("name", name); put("muscleGroup", "QUADRICEPS"); put("movementType", "LEGS")
            put("isCustom", if (custom) 1 else 0); put("archived", 0); put("createdAtMs", 0)
        },
    )

    @Test
    fun migrate1To2_addsNewLibraryExercises_withoutDuplicatesOrTouchingUserData() = runBlocking {
        val v1Count = ExerciseSeed.items.count { it.since == 1 }
        createVersion1("migration-test.db") { db ->
            ExerciseSeed.items.filter { it.since == 1 }.forEach { db.insertExercise(it.namePt, custom = false) }
            db.insertExercise("Goblet Squat", custom = true) // the user already created this one, in English
        }

        val room = Room.databaseBuilder(context, MyFitDatabase::class.java, "migration-test.db")
            .addMigrations(ExerciseSeed.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()
        val names = room.exerciseDao().getAll().map { it.name }

        val added = ExerciseSeed.items.count { it.since == 2 } - 1 // goblet squat already existed
        assertEquals(v1Count + 1 + added, names.size)
        assertEquals(1, names.count { it.equals("Goblet squat", ignoreCase = true) || it == "Agachamento goblet" })
        assertTrue("Prancha lateral" in names) // device language (pt-BR in tests)
        assertTrue(room.exerciseDao().getAll().single { it.name == "Goblet Squat" }.isCustom)
        assertEquals(2, room.openHelper.readableDatabase.version)
        room.close()
    }

    @Test
    fun freshInstall_getsWholeLibrary_once() = runBlocking {
        context.deleteDatabase("fresh-test.db")
        val room = Room.databaseBuilder(context, MyFitDatabase::class.java, "fresh-test.db")
            .addCallback(ExerciseSeed.Callback())
            .addMigrations(ExerciseSeed.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()
        assertEquals(ExerciseSeed.items.size, room.exerciseDao().getAll().size)
        room.close()
    }
}
