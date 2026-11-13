package io.github.luannamorim.myfit.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.luannamorim.myfit.platform.activity.ActivityServiceStarter
import io.github.luannamorim.myfit.platform.activity.AndroidActivityServiceStarter
import io.github.luannamorim.myfit.platform.session.AndroidRestAlarmScheduler
import io.github.luannamorim.myfit.platform.session.AndroidWorkoutServiceStarter
import io.github.luannamorim.myfit.platform.session.RestAlarmScheduler
import io.github.luannamorim.myfit.platform.session.RestSoundPlayer
import io.github.luannamorim.myfit.platform.session.SoundPoolRestSoundPlayer
import io.github.luannamorim.myfit.platform.session.WorkoutServiceStarter

@Module
@InstallIn(SingletonComponent::class)
abstract class PlatformModule {
    @Binds abstract fun bindRestAlarmScheduler(impl: AndroidRestAlarmScheduler): RestAlarmScheduler
    @Binds abstract fun bindWorkoutServiceStarter(impl: AndroidWorkoutServiceStarter): WorkoutServiceStarter
    @Binds abstract fun bindActivityServiceStarter(impl: AndroidActivityServiceStarter): ActivityServiceStarter
    @Binds abstract fun bindRestSoundPlayer(impl: SoundPoolRestSoundPlayer): RestSoundPlayer
}
