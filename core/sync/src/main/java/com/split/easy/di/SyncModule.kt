package com.split.easy.di

import android.content.Context
import androidx.work.WorkManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.memoryCacheSettings
import com.split.easy.domain.sync.SyncScheduler
import com.split.easy.sync.remote.FirestoreDataSource
import com.split.easy.sync.remote.RemoteDataSource
import com.split.easy.sync.upload.WorkManagerSyncScheduler
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {

    @Binds
    abstract fun bindRemoteDataSource(impl: FirestoreDataSource): RemoteDataSource

    @Binds
    abstract fun bindSyncScheduler(impl: WorkManagerSyncScheduler): SyncScheduler

    companion object {
        @Provides
        @Singleton
        fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
            WorkManager.getInstance(context)

        /**
         * Firestore without its own disk cache: Room is the only offline copy (M4-Q4). A small
         * in-memory cache remains, which live listeners need.
         */
        @Provides
        @Singleton
        fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance().apply {
            firestoreSettings = FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(memoryCacheSettings {})
                .build()
        }
    }
}
