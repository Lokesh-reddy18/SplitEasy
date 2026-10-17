package com.split.easy.domain.settlement

import com.split.easy.domain.model.Member
import com.split.easy.domain.model.Settlement
import com.split.easy.domain.model.SettlementMethod
import com.split.easy.domain.model.SettlementStatus
import com.split.easy.domain.model.SettlementStatus.CONFIRMED
import com.split.easy.domain.model.SettlementStatus.PENDING
import com.split.easy.domain.model.SettlementStatus.REJECTED
import com.split.easy.domain.money.MAX_AMOUNT_PAISE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettlementRulesTest {

    private val you = Member("you", userId = "uid-you")
    private val priya = Member("priya", userId = "uid-priya")
    private val meera = Member("meera", userId = "uid-meera")
    private val ravi = Member("ravi", userId = null) // guest
    private val arjun = Member("arjun", userId = null) // guest

    private fun settlement(
        from: Member,
        to: Member,
        status: SettlementStatus,
        isDeleted: Boolean = false,
    ) = Settlement("s1", from.id, to.id, 600_00, status, SettlementMethod.CASH, isDeleted)

    // Starting status (Q2, Q3, F3)

    @Test
    fun `payer records a payment to a real user - pending until the payee confirms`() {
        assertEquals(
            PENDING,
            SettlementRules.initialStatus(recorder = you, payer = you, payee = priya),
        )
    }

    @Test
    fun `payee records a payment - confirmed straight away`() {
        assertEquals(
            CONFIRMED,
            SettlementRules.initialStatus(recorder = priya, payer = you, payee = priya),
        )
        assertEquals(
            CONFIRMED,
            SettlementRules.initialStatus(recorder = you, payer = ravi, payee = you),
        )
    }

    @Test
    fun `payer records a payment to a guest - confirmed straight away`() {
        assertEquals(
            CONFIRMED,
            SettlementRules.initialStatus(recorder = you, payer = you, payee = ravi),
        )
    }

    @Test
    fun `third party can record a payment between two guests - confirmed`() {
        assertEquals(
            CONFIRMED,
            SettlementRules.initialStatus(recorder = you, payer = ravi, payee = arjun),
        )
    }

    @Test
    fun `third party can't record a payment involving a real user`() {
        assertNull(SettlementRules.initialStatus(recorder = you, payer = priya, payee = meera))
        assertNull(SettlementRules.initialStatus(recorder = you, payer = priya, payee = ravi))
        assertNull(SettlementRules.initialStatus(recorder = you, payer = ravi, payee = priya))
    }

    @Test
    fun `invalid recordings are refused`() {
        assertNull(SettlementRules.initialStatus(recorder = ravi, payer = ravi, payee = you))
        assertNull(SettlementRules.initialStatus(recorder = you, payer = you, payee = you))
        val removed = priya.copy(isRemoved = true)
        assertNull(SettlementRules.initialStatus(recorder = you, payer = you, payee = removed))
        assertNull(SettlementRules.initialStatus(recorder = removed, payer = removed, payee = you))
    }

    // Confirm, reject, cancel (F4)

    @Test
    fun `only the payee can confirm or reject, and only while pending`() {
        val pending = settlement(from = you, to = priya, status = PENDING)
        assertTrue(SettlementRules.canConfirmOrReject(priya, pending))
        assertFalse(SettlementRules.canConfirmOrReject(you, pending))
        assertFalse(SettlementRules.canConfirmOrReject(meera, pending))
        assertFalse(SettlementRules.canConfirmOrReject(priya, settlement(you, priya, CONFIRMED)))
        assertFalse(
            SettlementRules.canConfirmOrReject(
                priya,
                settlement(you, priya, PENDING, isDeleted = true),
            ),
        )
    }

    @Test
    fun `only the payer can cancel, and only while pending`() {
        val pending = settlement(from = you, to = priya, status = PENDING)
        assertTrue(SettlementRules.canCancel(you, pending))
        assertFalse(SettlementRules.canCancel(priya, pending))
        assertFalse(SettlementRules.canCancel(you, settlement(you, priya, REJECTED)))
        assertFalse(
            SettlementRules.canCancel(you, settlement(you, priya, PENDING, isDeleted = true)),
        )
    }

    // Delete (Q6, M1-Q6)

    @Test
    fun `payer or payee can delete a confirmed settlement`() {
        val confirmed = settlement(from = you, to = priya, status = CONFIRMED)
        assertTrue(SettlementRules.canDelete(you, confirmed, payer = you, payee = priya))
        assertTrue(SettlementRules.canDelete(priya, confirmed, payer = you, payee = priya))
        assertFalse(SettlementRules.canDelete(meera, confirmed, payer = you, payee = priya))
    }

    @Test
    fun `any member can delete a settlement between two guests`() {
        val confirmed = settlement(from = ravi, to = arjun, status = CONFIRMED)
        assertTrue(SettlementRules.canDelete(meera, confirmed, payer = ravi, payee = arjun))
    }

    @Test
    fun `pending, rejected, deleted, guest and removed cases can't delete`() {
        assertFalse(SettlementRules.canDelete(you, settlement(you, priya, PENDING), you, priya))
        assertFalse(SettlementRules.canDelete(you, settlement(you, priya, REJECTED), you, priya))
        assertFalse(
            SettlementRules.canDelete(
                you,
                settlement(you, priya, CONFIRMED, isDeleted = true),
                you,
                priya,
            ),
        )
        assertFalse(
            SettlementRules.canDelete(ravi, settlement(ravi, arjun, CONFIRMED), ravi, arjun),
        )
        val removed = meera.copy(isRemoved = true)
        assertFalse(
            SettlementRules.canDelete(removed, settlement(ravi, arjun, CONFIRMED), ravi, arjun),
        )
    }

    // Amount (Q5, section E)

    @Test
    fun `amount must be between one paisa and the payable amount`() {
        assertEquals(
            AmountCheck.NOT_POSITIVE,
            SettlementRules.checkAmount(0, payablePaise = 600_00),
        )
        assertEquals(
            AmountCheck.NOT_POSITIVE,
            SettlementRules.checkAmount(-1, payablePaise = 600_00),
        )
        assertEquals(AmountCheck.OK, SettlementRules.checkAmount(1, payablePaise = 600_00))
        assertEquals(AmountCheck.OK, SettlementRules.checkAmount(600_00, payablePaise = 600_00))
        assertEquals(
            AmountCheck.ABOVE_PAYABLE,
            SettlementRules.checkAmount(600_01, payablePaise = 600_00),
        )
    }

    @Test
    fun `amount can never exceed the cap`() {
        assertEquals(
            AmountCheck.ABOVE_LIMIT,
            SettlementRules.checkAmount(MAX_AMOUNT_PAISE + 1, payablePaise = Long.MAX_VALUE),
        )
        assertEquals(
            AmountCheck.OK,
            SettlementRules.checkAmount(MAX_AMOUNT_PAISE, MAX_AMOUNT_PAISE),
        )
    }
}
