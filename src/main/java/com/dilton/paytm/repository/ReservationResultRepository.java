package com.dilton.paytm.repository;

import com.dilton.paytm.entity.ReservationResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ReservationResultRepository
        extends JpaRepository<ReservationResult, ReservationResult.ReservationResultId> {

    List<ReservationResult> findByReservationId(UUID reservationId);
}