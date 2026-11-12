package io.github.luannamorim.myfit.data.repository

import io.github.luannamorim.myfit.core.time.TimeSource
import io.github.luannamorim.myfit.data.db.MyFitDatabase
import io.github.luannamorim.myfit.data.db.entity.SessionWithExercises
import io.github.luannamorim.myfit.domain.history.SessionDetail
import io.github.luannamorim.myfit.domain.history.SessionExerciseDetail
import io.github.luannamorim.myfit.domain.history.SessionSummary
import io.github.luannamorim.myfit.domain.model.Intensity
import io.github.luannamorim.myfit.domain.model.ItemStatus
import io.github.luannamorim.myfit.domain.model.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepository @Inject constructor(
    private val db: MyFitDatabase,
    private val time: TimeSource,
    private val backup: BackupTrigger,
) {
    private val dao = db.sessionDao()

    /** Finished sessions that started within [range] (local dates), newest first. */
    fun observeSummaries(range: ClosedRange<LocalDate>): Flow<List<SessionSummary>> =
        dao.observeFinished(range.start.toEpochDay(), range.endInclusive.toEpochDay()).map { list -> list.map { it.toSummary() } }

    fun observeLast(): Flow<SessionSummary?> = dao.observeLastFinished().map { it?.toSummary() }

    fun observeDetail(sessionId: Long): Flow<SessionDetail?> = dao.observeSession(sessionId).map { row ->
        if (row == null || row.session.status != SessionStatus.FINISHED) return@map null
        SessionDetail(
            summary = row.toSummary(),
            exercises = row.toActive().exercises.map { SessionExerciseDetail(it.name, it.status, it.sets) },
        )
    }

    suspend fun deleteSession(id: Long) {
        dao.deleteSession(id)
        backup.onDataChanged()
    }

    /** Replaces the photo of a finished session; returns the previous path (for the caller to delete the file). */
    suspend fun setImage(id: Long, imagePath: String?): String? {
        val previous = dao.getSession(id)?.session?.imagePath
        dao.setImagePath(id, imagePath)
        backup.onDataChanged()
        return previous
    }

    /** Sets (or clears, with null) the intensity of a finished session. */
    suspend fun setIntensity(id: Long, intensity: Intensity?) {
        dao.setIntensity(id, intensity?.name)
        backup.onDataChanged()
    }

    private fun SessionWithExercises.toSummary(): SessionSummary {
        val start = Instant.ofEpochMilli(session.started.wallMs).atZone(time.zone())
        val ordered = exercises.sortedBy { it.exercise.position }
        return SessionSummary(
            id = session.id,
            routineName = session.routineName,
            startDate = LocalDate.ofEpochDay(session.startDateEpochDay),
            startTime = start.toLocalTime().withSecond(0).withNano(0),
            durationMs = session.durationMs ?: 0,
            setsDone = ordered.sumOf { e -> e.sets.count { it.status == ItemStatus.DONE } },
            // Only exercises actually trained (at least one set done).
            exercises = ordered.filter { e -> e.sets.any { it.status == ItemStatus.DONE } }.map { it.exercise.name },
            intensity = session.intensity,
            imagePath = session.imagePath,
        )
    }
}
