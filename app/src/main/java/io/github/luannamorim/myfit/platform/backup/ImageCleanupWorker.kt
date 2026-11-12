package io.github.luannamorim.myfit.platform.backup

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.luannamorim.myfit.core.time.TimeSource
import io.github.luannamorim.myfit.data.backup.SnapshotMapper
import io.github.luannamorim.myfit.data.db.MyFitDatabase
import io.github.luannamorim.myfit.data.images.ImageStore
import java.util.concurrent.TimeUnit

/** Weekly removal of image files no row references anymore (older than a day). */
@HiltWorker
class ImageCleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val db: MyFitDatabase,
    private val images: ImageStore,
    private val time: TimeSource,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val referenced = SnapshotMapper.export(db).referencedImages()
        images.deleteOrphans(referenced, olderThanMs = time.now().wallMs - TimeUnit.DAYS.toMillis(1))
        return Result.success()
    }

    companion object {
        private const val NAME = "image-cleanup"

        fun schedule(context: Context) {
            runCatching { WorkManager.getInstance(context) }.getOrNull()?.enqueueUniquePeriodicWork(
                NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<ImageCleanupWorker>(7, TimeUnit.DAYS)
                    .setConstraints(Constraints.Builder().setRequiresDeviceIdle(true).setRequiresBatteryNotLow(true).build())
                    .build(),
            )
        }
    }
}
