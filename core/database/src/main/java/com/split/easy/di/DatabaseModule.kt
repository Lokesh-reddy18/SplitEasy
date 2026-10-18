package com.split.easy.di

import android.content.Context
import androidx.room.Room
import com.split.easy.data.local.SplitEasyDatabase
import com.split.easy.data.local.dao.ExpenseDao
import com.split.easy.data.local.dao.GroupDao
import com.split.easy.data.local.dao.GroupEventDao
import com.split.easy.data.local.dao.MemberDao
import com.split.easy.data.local.dao.SettlementDao
import com.split.easy.data.local.dao.SyncStatusDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SplitEasyDatabase =
        Room.databaseBuilder(context, SplitEasyDatabase::class.java, SplitEasyDatabase.NAME)
            .addMigrations(*SplitEasyDatabase.MIGRATIONS)
            .build()

    @Provides
    fun provideGroupDao(database: SplitEasyDatabase): GroupDao = database.groupDao()

    @Provides
    fun provideMemberDao(database: SplitEasyDatabase): MemberDao = database.memberDao()

    @Provides
    fun provideExpenseDao(database: SplitEasyDatabase): ExpenseDao = database.expenseDao()

    @Provides
    fun provideSettlementDao(database: SplitEasyDatabase): SettlementDao = database.settlementDao()

    @Provides
    fun provideGroupEventDao(database: SplitEasyDatabase): GroupEventDao = database.groupEventDao()

    @Provides
    fun provideSyncStatusDao(database: SplitEasyDatabase): SyncStatusDao = database.syncStatusDao()
}
