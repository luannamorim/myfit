package io.github.luannamorim.myfit.core.time

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

interface TimeSource {
    fun now(): Mark
    fun zone(): ZoneId
}

fun TimeSource.today(): LocalDate = java.time.Instant.ofEpochMilli(now().wallMs).atZone(zone()).toLocalDate()

@Singleton
class SystemTimeSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : TimeSource {
    override fun now(): Mark = Mark(
        wallMs = System.currentTimeMillis(),
        elapsedMs = SystemClock.elapsedRealtime(),
        bootCount = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, 0),
    )

    override fun zone(): ZoneId = ZoneId.systemDefault()
}
