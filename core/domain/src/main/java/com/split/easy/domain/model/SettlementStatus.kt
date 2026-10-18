package com.split.easy.domain.model

/** Only CONFIRMED settlements change balances. */
enum class SettlementStatus {
    PENDING,
    CONFIRMED,
    REJECTED,
}
