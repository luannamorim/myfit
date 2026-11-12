package io.github.luannamorim.myfit.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.luannamorim.myfit.data.backup.AndroidKeystoreKeyWrapper
import io.github.luannamorim.myfit.data.backup.KeyWrapper
import io.github.luannamorim.myfit.data.repository.BackupTrigger
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    @Named("settings")
    fun provideSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("settings") }

    @Provides
    @Singleton
    @Named("backup_key")
    fun provideBackupKeyDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("backup_key") }

    @Provides
    fun provideKeyWrapper(impl: AndroidKeystoreKeyWrapper): KeyWrapper = impl

    /** Replaced by the real scheduler in the backup stage. */
    @Provides
    @Singleton
    fun provideBackupTrigger(): BackupTrigger = BackupTrigger { }
}
