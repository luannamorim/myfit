package io.github.luannamorim.myfit.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.luannamorim.myfit.data.db.dao.ActivityDao
import io.github.luannamorim.myfit.data.db.dao.BackupDao
import io.github.luannamorim.myfit.data.db.dao.BodyDao
import io.github.luannamorim.myfit.data.db.dao.ExerciseDao
import io.github.luannamorim.myfit.data.db.dao.RoutineDao
import io.github.luannamorim.myfit.data.db.dao.SessionDao
import io.github.luannamorim.myfit.data.db.entity.ActivityEntity
import io.github.luannamorim.myfit.data.db.entity.BodyAssessmentEntity
import io.github.luannamorim.myfit.data.db.entity.BodyPhotoEntity
import io.github.luannamorim.myfit.data.db.entity.ExerciseEntity
import io.github.luannamorim.myfit.data.db.entity.RoutineEntity
import io.github.luannamorim.myfit.data.db.entity.RoutineExerciseEntity
import io.github.luannamorim.myfit.data.db.entity.SessionExerciseEntity
import io.github.luannamorim.myfit.data.db.entity.SessionSetEntity
import io.github.luannamorim.myfit.data.db.entity.WorkoutSessionEntity

@Database(
    entities = [
        ExerciseEntity::class,
        RoutineEntity::class,
        RoutineExerciseEntity::class,
        WorkoutSessionEntity::class,
        SessionExerciseEntity::class,
        SessionSetEntity::class,
        BodyAssessmentEntity::class,
        BodyPhotoEntity::class,
        ActivityEntity::class,
    ],
    version = MyFitDatabase.VERSION,
    exportSchema = true,
)
abstract class MyFitDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun routineDao(): RoutineDao
    abstract fun sessionDao(): SessionDao
    abstract fun bodyDao(): BodyDao
    abstract fun backupDao(): BackupDao
    abstract fun activityDao(): ActivityDao

    companion object {
        const val VERSION = 7
        const val NAME = "myfit.db"
    }
}
