package com.dilton.paytm.repository;

import com.dilton.paytm.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, java.util.UUID> {

    Optional<Reservation> findByShowIdAndUserIdAndIdempotencyKey(
            Long showId,
            String userId,
            String idempotencyKey
    );
	
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		SELECT r
		FROM Reservation r
		WHERE r.id = :id
	""")
	Optional<Reservation> findByIdForUpdate(@Param("id") UUID id);
}