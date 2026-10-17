package com.split.easy.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.split.easy.data.local.entity.ExpenseEntity
import com.split.easy.data.local.entity.ExpenseShareEntity
import com.split.easy.data.local.entity.GroupEntity
import com.split.easy.data.local.entity.GroupEventEntity
import com.split.easy.data.local.entity.MemberEntity
import com.split.easy.data.local.entity.SettlementEntity

/** The local database: the single source of truth. The UI only observes its Flows. */
@Database(
    entities = [
        GroupEntity::class,
        MemberEntity::class,
        ExpenseEntity::class,
        ExpenseShareEntity::class,
        SettlementEntity::class,
        GroupEventEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class SplitEasyDatabase : RoomDatabase() {
    companion object {
        const val NAME = "spliteasy.db"
    }
}
