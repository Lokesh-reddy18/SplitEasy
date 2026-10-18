package com.split.easy.data.repository

import com.split.easy.data.IdGenerator
import com.split.easy.data.local.DatabaseTest
import com.split.easy.domain.model.CurrentUser
import com.split.easy.domain.repository.CurrentUserProvider
import com.split.easy.domain.sync.SyncScheduler
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/** Real in-memory Room database with a fixed clock, predictable ids and a sync counter. */
abstract class RepositoryTest : DatabaseTest() {

    /** 10 Oct 2026, 09:00 UTC. Tests move it forward with [advanceClock]. */
    protected var now: Long = Instant.parse("2026-10-10T09:00:00Z").toEpochMilli()
        private set

    protected val clock: Clock
        get() = Clock.fixed(Instant.ofEpochMilli(now), ZoneOffset.UTC)

    protected fun advanceClock(millis: Long = 1_000) {
        now += millis
    }

    private var nextId = 0
    protected val ids = IdGenerator { "id-${++nextId}" }

    protected val you = CurrentUser(userId = "uid-you", displayName = "You")
    protected val currentUser = object : CurrentUserProvider {
        override fun currentUser() = you
    }

    protected var syncRequests = 0
    protected val syncScheduler = object : SyncScheduler {
        override fun requestSync() {
            syncRequests++
        }
    }

    /** Repositories read the clock when they write, so they're built with a clock that follows [now]. */
    protected val movingClock: Clock = object : Clock() {
        override fun getZone() = ZoneOffset.UTC

        override fun withZone(zone: java.time.ZoneId?) = this

        override fun instant(): Instant = Instant.ofEpochMilli(now)
    }
}
