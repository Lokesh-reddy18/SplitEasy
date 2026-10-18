package com.split.easy.domain.model

/** How a settlement was paid. It decides which confirmation message the payee sees. */
enum class SettlementMethod {
    UPI,
    CASH,
}
