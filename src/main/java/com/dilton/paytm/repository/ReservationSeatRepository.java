package com.dilton.paytm.repository;

import com.dilton.paytm.entity.ReservationSeat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationSeatRepository extends JpaRepository<ReservationSeat, ReservationSeat.ReservationSeatId> {
}