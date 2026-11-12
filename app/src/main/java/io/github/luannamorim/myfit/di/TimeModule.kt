package io.github.luannamorim.myfit.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.luannamorim.myfit.core.time.SystemTimeSource
import io.github.luannamorim.myfit.core.time.TimeSource

@Module
@InstallIn(SingletonComponent::class)
abstract class TimeModule {
    @Binds
    abstract fun bindTimeSource(impl: SystemTimeSource): TimeSource
}
