package com.dilton.paytm.repository;

import com.dilton.paytm.entity.ReservationSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;

public interface ReservationSeatRepository extends JpaRepository<ReservationSeat, ReservationSeat.ReservationSeatId> {
	List<ReservationSeat> findByReservationId(UUID reservationId);
}