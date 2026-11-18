package org.example.tordemo.exitnode.helpers

import groovy.transform.CompileStatic

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicReference

/** A thread-safe test clock that advances without waiting for real time. */
@CompileStatic
final class MutableClock extends Clock {

    private final AtomicReference<Instant> now
    private final ZoneId zone

    MutableClock() {
        this(new AtomicReference<Instant>(Instant.parse('2026-11-13T12:00:00Z')), ZoneOffset.UTC)
    }

    private MutableClock(AtomicReference<Instant> now, ZoneId zone) {
        this.now = now
        this.zone = zone
    }

    void advance(Duration duration) {
        now.updateAndGet { Instant instant -> instant.plus(duration) }
    }

    @Override
    Instant instant() {
        now.get()
    }

    @Override
    ZoneId getZone() {
        zone
    }

    @Override
    Clock withZone(ZoneId zone) {
        new MutableClock(now, Objects.requireNonNull(zone))
    }
}
