package io.github.luannamorim.myfit.data.repository

/** Notified after every important data change so a debounced backup can be scheduled. */
fun interface BackupTrigger {
    fun onDataChanged()

    /** Cancels scheduled backups (used before a restore replaces the data). */
    fun cancelPending() {}
}
