package io.github.luannamorim.myfit

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import io.github.luannamorim.myfit.data.backup.BackupManager
import io.github.luannamorim.myfit.di.ApplicationScope
import io.github.luannamorim.myfit.platform.backup.BackupScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MyFitApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var backupScheduler: Lazy<BackupScheduler>
    @Inject lateinit var backupManager: Lazy<BackupManager>
    @Inject @ApplicationScope lateinit var appScope: CoroutineScope

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            backupManager.get().recoverInterruptedRestore()
            backupScheduler.get().ensurePeriodic()
        }
    }
}
