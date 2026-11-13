package io.github.luannamorim.myfit.testutil

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.luannamorim.myfit.core.time.FakeTimeSource
import io.github.luannamorim.myfit.data.repository.BackupTrigger
import io.github.luannamorim.myfit.data.repository.SessionRepository
import io.github.luannamorim.myfit.platform.session.AndroidRestAlarmScheduler
import io.github.luannamorim.myfit.platform.session.SessionController
import io.github.luannamorim.myfit.platform.session.SessionNotifications
import io.github.luannamorim.myfit.platform.session.WorkoutServiceStarter

class FakeServiceStarter : WorkoutServiceStarter {
    var running = false
    override fun start() { running = true }
    override fun stop() { running = false }
}

/** Real repository/controller wiring over an in-memory database and a fake clock. */
class SessionEnv {
    val context: Context = ApplicationProvider.getApplicationContext()
    val db = inMemoryDb()
    val time = FakeTimeSource()
    val repo = SessionRepository(db, BackupTrigger { })
    val notifications = SessionNotifications(context)
    val starter = FakeServiceStarter()
    val controller = SessionController(repo, time, AndroidRestAlarmScheduler(context, notifications, time), starter, notifications)
    val settings = testSettings()

    /** Creates a routine with [setsPerExercise] sets per exercise (12 reps, 40 kg, 90 s) and starts it. */
    suspend fun startWorkout(vararg setsPerExercise: Int): Long {
        val ids = setsPerExercise.mapIndexed { i, _ -> db.insertExercise("Exercício ${i + 1}") }
        val routine = db.insertRoutine("Costas", ids)
        // Adjust the number of sets per item.
        val items = db.routineDao().getAllItems().filter { it.routineId == routine }.sortedBy { it.position }
        db.routineDao().deleteItems(routine)
        db.routineDao().insertItems(items.mapIndexed { i, item -> item.copy(id = 0, sets = setsPerExercise[i]) })
        return controller.start(routine)
    }

    fun close() = db.close()
}
