package io.github.luannamorim.myfit.data.db.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.luannamorim.myfit.core.time.Mark
import io.github.luannamorim.myfit.domain.model.ItemStatus
import io.github.luannamorim.myfit.domain.model.MovementType
import io.github.luannamorim.myfit.domain.model.MuscleGroup
import io.github.luannamorim.myfit.domain.model.SessionStatus

@Entity(
    tableName = "workout_session",
    foreignKeys = [
        ForeignKey(entity = RoutineEntity::class, parentColumns = ["id"], childColumns = ["routineId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("routineId"), Index("status"), Index("startDateEpochDay")],
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val routineId: Long?,
    /** Snapshot: history keeps the name even if the routine is renamed or deleted. */
    val routineName: String,
    val status: SessionStatus,
    @Embedded(prefix = "started_") val started: Mark,
    @Embedded(prefix = "paused_") val paused: Mark? = null,
    val totalPausedMs: Long = 0,
    @Embedded(prefix = "ended_") val ended: Mark? = null,
    val durationMs: Long? = null,
    val cursorExercise: Int = 0,
    val cursorSet: Int = 0,
    @Embedded(prefix = "rest_") val restStarted: Mark? = null,
    val restDurationMs: Long? = null,
    /** Local date the session started on (calendar/history key). */
    val startDateEpochDay: Long,
)

@Entity(
    tableName = "session_exercise",
    foreignKeys = [
        ForeignKey(entity = WorkoutSessionEntity::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ExerciseEntity::class, parentColumns = ["id"], childColumns = ["exerciseId"], onDelete = ForeignKey.SET_NULL),
    ],
    indices = [Index("sessionId"), Index("exerciseId")],
)
data class SessionExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long?,
    val position: Int,
    val name: String,
    val muscleGroup: MuscleGroup,
    val movementType: MovementType,
    val imagePath: String? = null,
    val restSeconds: Int,
    val notes: String? = null,
    val status: ItemStatus = ItemStatus.PENDING,
    val seedKey: String? = null,
)

@Entity(
    tableName = "session_set",
    foreignKeys = [
        ForeignKey(entity = SessionExerciseEntity::class, parentColumns = ["id"], childColumns = ["sessionExerciseId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("sessionExerciseId")],
)
data class SessionSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionExerciseId: Long,
    val setIndex: Int,
    val plannedReps: Int,
    val plannedLoadKg: Double? = null,
    val actualReps: Int? = null,
    val actualLoadKg: Double? = null,
    val status: ItemStatus = ItemStatus.PENDING,
    val completedAtWallMs: Long? = null,
)
