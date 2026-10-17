package com.split.easy.di

import com.split.easy.data.IdGenerator
import com.split.easy.data.UuidGenerator
import com.split.easy.data.repository.OfflineFirstExpenseRepository
import com.split.easy.data.repository.OfflineFirstGroupRepository
import com.split.easy.data.repository.OfflineFirstMemberRepository
import com.split.easy.data.repository.OfflineFirstSettlementRepository
import com.split.easy.data.repository.RoomSyncStatusRepository
import com.split.easy.data.user.LocalCurrentUserProvider
import com.split.easy.domain.repository.CurrentUserProvider
import com.split.easy.domain.repository.ExpenseRepository
import com.split.easy.domain.repository.GroupRepository
import com.split.easy.domain.repository.MemberRepository
import com.split.easy.domain.repository.SettlementRepository
import com.split.easy.domain.repository.SyncStatusRepository
import com.split.easy.sync.NoOpSyncScheduler
import com.split.easy.sync.SyncScheduler
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindGroupRepository(impl: OfflineFirstGroupRepository): GroupRepository

    @Binds
    abstract fun bindMemberRepository(impl: OfflineFirstMemberRepository): MemberRepository

    @Binds
    abstract fun bindExpenseRepository(impl: OfflineFirstExpenseRepository): ExpenseRepository

    @Binds
    abstract fun bindSettlementRepository(
        impl: OfflineFirstSettlementRepository,
    ): SettlementRepository

    @Binds
    abstract fun bindSyncStatusRepository(impl: RoomSyncStatusRepository): SyncStatusRepository

    companion object {
        @Provides
        @Singleton
        fun provideClock(): Clock = Clock.systemDefaultZone()

        @Provides
        @Singleton
        fun provideIdGenerator(): IdGenerator = UuidGenerator()

        @Provides
        @Singleton
        fun provideCurrentUserProvider(): CurrentUserProvider = LocalCurrentUserProvider()

        /** No backend in M2 (M2-Q5); M4 binds the WorkManager scheduler here instead. */
        @Provides
        @Singleton
        fun provideSyncScheduler(): SyncScheduler = NoOpSyncScheduler()
    }
}
