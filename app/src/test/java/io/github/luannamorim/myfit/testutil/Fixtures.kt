package io.github.luannamorim.myfit.testutil

import io.github.luannamorim.myfit.data.db.entity.ActivityEntity
import io.github.luannamorim.myfit.data.db.entity.BodyAssessmentEntity
import io.github.luannamorim.myfit.data.db.entity.BodyPhotoEntity
import io.github.luannamorim.myfit.domain.activity.Sport
import io.github.luannamorim.myfit.domain.model.Intensity
import io.github.luannamorim.myfit.domain.model.PhotoPose
import io.github.luannamorim.myfit.domain.session.SessionEngine

/** Fills every table: routine + finished session + active session + assessment + photo. */
suspend fun SessionEnv.populateEverything(imagePaths: List<String> = emptyList(), sessionImage: String? = null, activityImage: String? = null) {
    startWorkout(2, 1)
    var s = repo.getActive()!!
    s = SessionEngine.completeSet(s, 12, 40.0, time.now())
    s = SessionEngine.skipSet(s, time.now())
    time.advance(1_200_000)
    repo.finish(s, time.now(), updateRoutineLoads = true, intensity = Intensity.HIGH, imagePath = sessionImage)
    startWorkout(1)
    db.bodyDao().insertAssessment(BodyAssessmentEntity(dateEpochDay = 20_400, weightKg = 73.2, heightCm = 178.0, waistCm = 79.0, notes = "manhã", createdAtMs = 1, updatedAtMs = 2))
    imagePaths.forEach { db.bodyDao().insertPhoto(BodyPhotoEntity(dateEpochDay = 20_400, imagePath = it, pose = PhotoPose.FRONT, createdAtMs = 3)) }
    db.activityDao().insert(
        ActivityEntity(
            sport = Sport.SWIMMING, dateEpochDay = 20_401, durationMinutes = 45, distanceKm = 1.5,
            intensity = Intensity.MEDIUM, notes = "piscina", imagePath = activityImage, createdAtMs = 4,
        ),
    )
}
