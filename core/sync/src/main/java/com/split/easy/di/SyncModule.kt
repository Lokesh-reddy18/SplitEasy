package com.split.easy.di

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.memoryCacheSettings
import com.split.easy.sync.remote.FirestoreDataSource
import com.split.easy.sync.remote.RemoteDataSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {

    @Binds
    abstract fun bindRemoteDataSource(impl: FirestoreDataSource): RemoteDataSource

    companion object {
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
