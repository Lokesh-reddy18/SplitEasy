package com.split.easy.di

import com.google.firebase.auth.FirebaseAuth
import com.split.easy.data.IdGenerator
import com.split.easy.data.UuidGenerator
import com.split.easy.data.repository.OfflineFirstExpenseRepository
import com.split.easy.data.repository.OfflineFirstGroupRepository
import com.split.easy.data.repository.OfflineFirstMemberRepository
import com.split.easy.data.repository.OfflineFirstSettlementRepository
import com.split.easy.data.repository.RoomSyncStatusRepository
import com.split.easy.data.user.FirebaseAuthRepository
import com.split.easy.data.user.FirebaseCurrentUserProvider
import com.split.easy.domain.repository.AuthRepository
import com.split.easy.domain.repository.CurrentUserProvider
import com.split.easy.domain.repository.ExpenseRepository
import com.split.easy.domain.repository.GroupRepository
import com.split.easy.domain.repository.MemberRepository
import com.split.easy.domain.repository.SettlementRepository
import com.split.easy.domain.repository.SyncStatusRepository
import com.split.easy.domain.sync.SyncScheduler
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
    abstract fun bindAuthRepository(impl: FirebaseAuthRepository): AuthRepository

    @Binds
    abstract fun bindCurrentUserProvider(impl: FirebaseCurrentUserProvider): CurrentUserProvider

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
        fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()
    }
}
