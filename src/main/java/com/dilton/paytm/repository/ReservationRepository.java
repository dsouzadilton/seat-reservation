package com.dilton.paytm.repository;

import com.dilton.paytm.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, java.util.UUID> {

    Optional<Reservation> findByShowIdAndUserIdAndIdempotencyKey(
            Long showId,
            String userId,
            String idempotencyKey
    );
}