package com.dilton.paytm.repository;

import com.dilton.paytm.entity.Seat;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT s
        FROM Seat s
        WHERE s.showId = :showId
          AND s.seatNumber IN :seatNumbers
        ORDER BY s.seatNumber
    """)
    List<Seat> findSeatsForUpdate(
            @Param("showId") Long showId,
            @Param("seatNumbers") List<String> seatNumbers
    );
	
	List<Seat> findByShowIdOrderBySeatNumber(Long showId);
	
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
		SELECT s
		FROM Seat s
		WHERE s.id IN :seatIds
		ORDER BY s.seatNumber
	""")
	List<Seat> findSeatsForUpdateByIds(
			@Param("seatIds") List<Long> seatIds
	);
	
	@Query("""
		SELECT COUNT(s)
		FROM Seat s
		WHERE s.showId = :showId
		  AND s.status = 'available'
	""")
	long countAvailableSeatsByShowId(@Param("showId") Long showId);
}