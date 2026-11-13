package org.example.tordemo.exitnode.helpers;

import java.time.*;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/** A thread-safe test clock that advances without waiting for real time. */
public final class MutableClock extends Clock {

    private final AtomicReference<Instant> now;
    private final ZoneId zone;

    public MutableClock() {
        this(new AtomicReference<>(Instant.parse("2026-11-13T12:00:00Z")), ZoneOffset.UTC);
    }

    private MutableClock(AtomicReference<Instant> now, ZoneId zone) {
        this.now = now;
        this.zone = zone;
    }

    public void advance(Duration duration) {
        now.updateAndGet(instant -> instant.plus(duration));
    }

    @Override
    public Instant instant() {
        return now.get();
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return new MutableClock(now, Objects.requireNonNull(zone));
    }
}
