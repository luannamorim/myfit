package io.github.luannamorim.myfit.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.luannamorim.myfit.data.db.seed.ExerciseSeed
import io.github.luannamorim.myfit.domain.model.ExerciseMeasure
import io.github.luannamorim.myfit.domain.model.TimedSets

/** Every schema migration, oldest first. Each one has a case in `MigrationTest`. */
object Migrations {
    /** v4: intensity and photo of a finished workout, and the log of other sports (`activity`). */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE workout_session ADD COLUMN intensity TEXT")
            db.execSQL("ALTER TABLE workout_session ADD COLUMN imagePath TEXT")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `activity` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sport` TEXT NOT NULL, " +
                    "`name` TEXT, `dateEpochDay` INTEGER NOT NULL, `durationMinutes` INTEGER NOT NULL, `distanceKm` REAL, " +
                    "`intensity` TEXT, `notes` TEXT, `imagePath` TEXT, `createdAtMs` INTEGER NOT NULL)",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_dateEpochDay` ON `activity` (`dateEpochDay`)")
        }
    }

    /**
     * v5: exercises (and session snapshots) say whether a set counts reps or seconds. Library holds
     * and cardio become timed; their workout targets that were clearly reps get a time default.
     * Past sessions keep what was recorded (as reps).
     */
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE exercise ADD COLUMN measure TEXT NOT NULL DEFAULT 'REPS'")
            db.execSQL("ALTER TABLE session_exercise ADD COLUMN measure TEXT NOT NULL DEFAULT 'REPS'")
            val timed = ExerciseSeed.items.filter { it.measure == ExerciseMeasure.TIME }
            if (timed.isEmpty()) return
            val keys = timed.joinToString(",") { "'${it.key}'" }
            db.execSQL("UPDATE exercise SET measure = 'TIME' WHERE seedKey IN ($keys)")
            timed.forEach { item ->
                db.execSQL(
                    "UPDATE routine_exercise SET reps = ? WHERE reps < $REPS_LIKE_BELOW AND exerciseId IN (SELECT id FROM exercise WHERE seedKey = ?)",
                    arrayOf<Any>(TimedSets.defaultSeconds(item.muscleGroup), item.key),
                )
            }
        }
    }

    /** v6: assessments record height (for BMI and the body-fat estimate). */
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE body_assessment ADD COLUMN heightCm REAL")
        }
    }

    /** Targets below this are taken as repetitions when an exercise becomes timed. */
    private const val REPS_LIKE_BELOW = 15

    val ALL: Array<Migration> = arrayOf(*ExerciseSeed.MIGRATIONS, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
}
