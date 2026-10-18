package com.split.easy.data.repository

import androidx.room.withTransaction
import com.split.easy.data.IdGenerator
import com.split.easy.data.local.SplitEasyDatabase
import com.split.easy.data.local.entity.SettlementEntity
import com.split.easy.data.mapper.toDomain
import com.split.easy.domain.model.Settlement
import com.split.easy.domain.model.SettlementMethod
import com.split.easy.domain.model.SettlementStatus
import com.split.easy.domain.repository.CurrentUserProvider
import com.split.easy.domain.repository.SettlementRepository
import com.split.easy.domain.sync.SyncScheduler
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfflineFirstSettlementRepository @Inject constructor(
    private val db: SplitEasyDatabase,
    private val currentUser: CurrentUserProvider,
    private val ids: IdGenerator,
    private val clock: Clock,
    private val syncScheduler: SyncScheduler,
) : SettlementRepository {

    private val settlementDao = db.settlementDao()

    override fun observeSettlements(groupId: String): Flow<List<Settlement>> =
        settlementDao.observeSettlements(groupId).map { settlements ->
            settlements.map { it.toDomain() }
        }

    override suspend fun recordSettlement(
        groupId: String,
        fromMemberId: String,
        toMemberId: String,
        amountPaise: Long,
        method: SettlementMethod,
        status: SettlementStatus,
        upiTxnId: String?,
    ): String {
        val now = clock.millis()
        val settlement = SettlementEntity(
            id = ids.newId(),
            groupId = groupId,
            fromMemberId = fromMemberId,
            toMemberId = toMemberId,
            amountPaise = amountPaise,
            date = LocalDate.now(clock),
            upiTxnId = upiTxnId,
            status = status,
            method = method,
            updatedAt = now,
        )
        db.withTransaction {
            settlementDao.upsert(settlement)
            db.groupDao().touchActivity(groupId, now)
        }
        syncScheduler.requestSync()
        return settlement.id
    }

    override suspend fun setStatus(settlementId: String, status: SettlementStatus) {
        val now = clock.millis()
        db.withTransaction {
            val settlement =
                requireNotNull(settlementDao.getSettlement(settlementId)) {
                    "No settlement $settlementId"
                }
            settlementDao.updateStatus(settlementId, status, updatedAt = now)
            db.groupDao().touchActivity(settlement.groupId, now)
        }
        syncScheduler.requestSync()
    }

    override suspend fun deleteSettlement(settlementId: String) {
        val now = clock.millis()
        db.withTransaction {
            val settlement =
                requireNotNull(settlementDao.getSettlement(settlementId)) {
                    "No settlement $settlementId"
                }
            settlementDao.softDelete(settlementId, updatedAt = now)
            db.groupDao().touchActivity(settlement.groupId, now)
        }
        syncScheduler.requestSync()
    }

    override fun observeWaitingForMyConfirmation(): Flow<Map<String, Int>> =
        settlementDao.observeWaitingForConfirmation(currentUser.currentUser().userId)
            .map { counts -> counts.associate { it.groupId to it.count } }
}
