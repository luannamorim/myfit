package io.github.luannamorim.myfit.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.luannamorim.myfit.data.db.Migrations
import io.github.luannamorim.myfit.data.db.MyFitDatabase
import io.github.luannamorim.myfit.data.db.seed.ExerciseSeed
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MyFitDatabase =
        Room.databaseBuilder(context, MyFitDatabase::class.java, MyFitDatabase.NAME)
            .addCallback(ExerciseSeed.Callback())
            .addMigrations(*Migrations.ALL)
            .build()

    @Provides fun provideExerciseDao(db: MyFitDatabase) = db.exerciseDao()
    @Provides fun provideRoutineDao(db: MyFitDatabase) = db.routineDao()
    @Provides fun provideSessionDao(db: MyFitDatabase) = db.sessionDao()
    @Provides fun provideBodyDao(db: MyFitDatabase) = db.bodyDao()
}
