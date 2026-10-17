package com.lokeshreddy.spliteasy.domain.model

/** Only CONFIRMED settlements change balances. */
enum class SettlementStatus {
    PENDING,
    CONFIRMED,
    REJECTED,
}
