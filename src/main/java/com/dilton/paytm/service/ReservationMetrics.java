package com.dilton.paytm.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ReservationMetrics {

    private final MeterRegistry meterRegistry;

    private final Counter confirmedCounter;
    private final Counter declinedSeatTakenCounter;
    private final Counter declinedPerUserLimitCounter;
    private final Counter idempotentReplayCounter;

    private final Map<Long, AtomicInteger> availableSeatsByShow =
            new ConcurrentHashMap<>();

    public ReservationMetrics(MeterRegistry meterRegistry) {

        this.meterRegistry = meterRegistry;

        confirmedCounter = Counter.builder("reservations_confirmed_total")
                .description("Number of confirmed reservations")
                .register(meterRegistry);

        declinedSeatTakenCounter = Counter.builder("reservations_declined_total")
                .tag("reason", "seat-taken")
                .description("Reservations declined because seats were already taken")
                .register(meterRegistry);

        declinedPerUserLimitCounter = Counter.builder("reservations_declined_total")
                .tag("reason", "per-user-limit")
                .description("Reservations declined because of the per-user limit")
                .register(meterRegistry);

        idempotentReplayCounter = Counter.builder("reservations_declined_total")
                .tag("reason", "idempotent-replay")
                .description("Idempotent reservation replays")
                .register(meterRegistry);
    }

    public void reservationConfirmed() {
        confirmedCounter.increment();
    }

    public void seatTakenDeclined() {
        declinedSeatTakenCounter.increment();
    }

    public void perUserLimitDeclined() {
        declinedPerUserLimitCounter.increment();
    }

    public void idempotentReplay() {
        idempotentReplayCounter.increment();
    }

    public void setSeatsAvailable(Long showId, long count) {

        AtomicInteger gauge = availableSeatsByShow.computeIfAbsent(
                showId,
                id -> {
                    AtomicInteger value = new AtomicInteger();

                    Gauge.builder(
                                    "seats_available",
                                    value,
                                    AtomicInteger::get
                            )
                            .tag("show_id", String.valueOf(id))
                            .description("Current number of available seats for a show")
                            .register(meterRegistry);

                    return value;
                }
        );

        gauge.set((int) count);
    }
}