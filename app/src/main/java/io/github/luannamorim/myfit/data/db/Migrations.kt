package io.github.luannamorim.myfit.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.luannamorim.myfit.data.db.seed.ExerciseSeed

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

    val ALL: Array<Migration> = arrayOf(*ExerciseSeed.MIGRATIONS, MIGRATION_3_4)
}
