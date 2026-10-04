package com.dilton.paytm.service;

import com.dilton.paytm.dto.ReserveRequest;
import com.dilton.paytm.dto.DeclinedSeat;
import com.dilton.paytm.dto.ReserveResponse;
import com.dilton.paytm.entity.ReservationResult;
import com.dilton.paytm.entity.Show;
import com.dilton.paytm.entity.Reservation;
import com.dilton.paytm.entity.UserShowLimit;
import com.dilton.paytm.entity.Seat;
import com.dilton.paytm.entity.ReservationSeat;
import com.dilton.paytm.repository.ReservationRepository;
import com.dilton.paytm.repository.ReservationSeatRepository;
import com.dilton.paytm.repository.SeatRepository;
import com.dilton.paytm.repository.ShowRepository;
import com.dilton.paytm.repository.UserShowLimitRepository;
import com.dilton.paytm.repository.ReservationResultRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.ArrayList;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class ReservationService {

    private final ShowRepository showRepository;
    private final SeatRepository seatRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationSeatRepository reservationSeatRepository;
    private final UserShowLimitRepository userShowLimitRepository;
	private final ReservationResultRepository reservationResultRepository;
	private final ReservationMetrics reservationMetrics;
	private static final Logger log = LoggerFactory.getLogger(ReservationService.class);
		
    public ReservationService(
            ShowRepository showRepository,
            SeatRepository seatRepository,
            ReservationRepository reservationRepository,
            ReservationSeatRepository reservationSeatRepository,
            UserShowLimitRepository userShowLimitRepository,
			ReservationResultRepository reservationResultRepository,
			ReservationMetrics reservationMetrics
    ) {
        this.showRepository = showRepository;
        this.seatRepository = seatRepository;
        this.reservationRepository = reservationRepository;
        this.reservationSeatRepository = reservationSeatRepository;
        this.userShowLimitRepository = userShowLimitRepository;
		this.reservationResultRepository = reservationResultRepository;
		this.reservationMetrics = reservationMetrics;
    }

    @Transactional
	public ReserveResponse reserve(Long showId, String userId, ReserveRequest request) {
		Show show = showRepository.findById(showId)
				.orElseThrow(() -> new IllegalArgumentException("Show not found"));

		if (request.seats() == null || request.seats().isEmpty()) {
			throw new IllegalArgumentException("At least one seat is required");
		}

		if (request.idempotencyKey() == null || request.idempotencyKey().isBlank()) {
			throw new IllegalArgumentException("Idempotency key is required");
		}
		
		List<String> requestedSeats = request.seats().stream()
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .distinct()
        .sorted()
        .toList();
		
		if (requestedSeats.isEmpty()) {
			throw new IllegalArgumentException("At least one valid seat is required");
		}
		
		String bodyHash = calculateBodyHash(requestedSeats);

		Reservation existing = reservationRepository
        .findByShowIdAndUserIdAndIdempotencyKey(
                showId,
                userId,
                request.idempotencyKey()
        )
        .orElse(null);

		if (existing != null) {
			if (!existing.getBodyHash().equals(bodyHash)) {
				throw new IllegalStateException("Idempotency key already used with a different request");
			}
			reservationMetrics.idempotentReplay();

			log.info(
				"event=idempotent_replay showId={} userId={} reservationId={} idempotencyKey={}",
				showId,
				userId,
				existing.getId(),
				request.idempotencyKey()
			);

			return buildResponse(existing);
		}
		
		String userShowLockKey = "show:" + showId + ":user:" + userId;

		userShowLimitRepository.acquireUserShowLock(userShowLockKey);
		existing = reservationRepository
			.findByShowIdAndUserIdAndIdempotencyKey(
				showId, userId, request.idempotencyKey()
			)
			.orElse(null);

		if (existing != null) {
			if (!existing.getBodyHash().equals(bodyHash)) {
				throw new IllegalStateException(
					"Idempotency key already used with a different request"
				);
			}

			reservationMetrics.idempotentReplay();

			log.info(
				"event=idempotent_replay showId={} userId={} reservationId={} idempotencyKey={}",
				showId,
				userId,
				existing.getId(),
				request.idempotencyKey()
			);

			return buildResponse(existing);
		}
		UserShowLimit userShowLimit = userShowLimitRepository
        .findByShowIdAndUserId(showId, userId)
        .orElseGet(() -> {
            UserShowLimit newLimit = new UserShowLimit();
            newLimit.setShowId(showId);
            newLimit.setUserId(userId);
            newLimit.setSeatCount(0);
            return userShowLimitRepository.save(newLimit);
        });

		int currentSeatCount = userShowLimit.getSeatCount();


		
		List<Seat> seats = seatRepository.findSeatsForUpdate(showId, requestedSeats);

		if (seats.size() != requestedSeats.size()) {
			throw new IllegalArgumentException("One or more requested seats do not exist");
		}
		
		List<Seat> confirmedSeats = new ArrayList<>();
		List<DeclinedSeat> declinedSeats = new ArrayList<>();

		int allocatedCount = currentSeatCount;

		for (Seat seat : seats) {
			if (!"available".equals(seat.getStatus())) {
				declinedSeats.add(
					new DeclinedSeat(seat.getSeatNumber(), "seat_taken")
				);

				reservationMetrics.seatTakenDeclined();

				log.info(
					"event=reservation_declined showId={} userId={} seat={} reason=seat-taken",
					showId,
					userId,
					seat.getSeatNumber()
				);

				continue;
			}

			if (allocatedCount >= show.getPerUserLimit()) {
				declinedSeats.add(
					new DeclinedSeat(seat.getSeatNumber(), "per_user_limit")
				);

				reservationMetrics.perUserLimitDeclined();

				log.info(
					"event=reservation_declined showId={} userId={} seat={} reason=per-user-limit",
					showId,
					userId,
					seat.getSeatNumber()
				);

				continue;
			}

			seat.setStatus("confirmed");
			confirmedSeats.add(seat);
			allocatedCount++;
		}
		
		userShowLimit.setSeatCount(allocatedCount);
		userShowLimitRepository.save(userShowLimit);
		
		if (confirmedSeats.isEmpty()) {
			throw new IllegalStateException("No requested seats could be reserved");
		}
		
		Reservation reservation = new Reservation();

		reservation.setId(UUID.randomUUID());
		reservation.setShowId(showId);
		reservation.setUserId(userId);
		reservation.setIdempotencyKey(request.idempotencyKey());
		reservation.setBodyHash(bodyHash);
		reservation.setStatus("confirmed");
		reservation.setAmountPaise(
				show.getPricePaise() * confirmedSeats.size()
		);
		reservation.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
		reservation.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

		reservation = reservationRepository.save(reservation);
		reservationMetrics.reservationConfirmed();
		for (Seat seat : confirmedSeats) {
			ReservationSeat reservationSeat = new ReservationSeat();

			reservationSeat.setReservationId(reservation.getId());
			reservationSeat.setSeatId(seat.getId());

			reservationSeatRepository.save(reservationSeat);
		}
		
		for (String seatNumber : requestedSeats) {
			ReservationResult result = new ReservationResult();
			result.setReservationId(reservation.getId());
			result.setSeatNumber(seatNumber);

			boolean confirmed = confirmedSeats.stream()
				.anyMatch(seat -> seat.getSeatNumber().equals(seatNumber));

			result.setResult(confirmed ? "confirmed" : "declined");

			if (!confirmed) {
				DeclinedSeat declined = declinedSeats.stream()
					.filter(d -> d.seat().equals(seatNumber))
					.findFirst()
					.orElseThrow();

				result.setReason(declined.reason());
			}

			reservationResultRepository.save(result);
		}
		
		List<String> confirmedSeatNumbers = confirmedSeats.stream()
			.map(Seat::getSeatNumber)
			.toList();

		String status = declinedSeats.isEmpty()
			? "confirmed"
			: "partially_confirmed";
			
		log.info(
			"event=reservation_confirmed showId={} userId={} reservationId={} confirmedSeats={} declinedSeats={} amountPaise={} status={}",
			showId,
			userId,
			reservation.getId(),
			confirmedSeatNumbers,
			declinedSeats,
			reservation.getAmountPaise(),
			status
		);

		return new ReserveResponse(
			reservation.getId(),
			reservation.getShowId(),
			reservation.getUserId(),
			confirmedSeatNumbers,
			declinedSeats,
			reservation.getAmountPaise(),
			status
		);
	}
	
	@Transactional
	public void cancel(UUID reservationId, String userId) {

		Reservation reservation = reservationRepository
				.findByIdForUpdate(reservationId)
				.orElseThrow(() ->
						new IllegalArgumentException("Reservation not found"));

		if (!reservation.getUserId().equals(userId)) {
			throw new IllegalStateException(
					"You are not allowed to cancel this reservation"
			);
		}

		if ("cancelled".equals(reservation.getStatus())) {
			return;
		}

		List<ReservationSeat> reservationSeats =
				reservationSeatRepository.findByReservationId(reservationId);

		List<Long> seatIds = reservationSeats.stream()
				.map(ReservationSeat::getSeatId)
				.sorted()
				.toList();

		List<Seat> seats =
				seatRepository.findSeatsForUpdateByIds(seatIds);

		String userShowLockKey =
				"show:" + reservation.getShowId() + ":user:" + userId;

		userShowLimitRepository.acquireUserShowLock(userShowLockKey);

		UserShowLimit userShowLimit =
				userShowLimitRepository
						.findByShowIdAndUserId(
								reservation.getShowId(),
								userId
						)
						.orElseThrow(() ->
								new IllegalStateException(
										"User show limit record not found"
								));

		for (Seat seat : seats) {
			if ("confirmed".equals(seat.getStatus())) {
				seat.setStatus("available");
			}
		}

		int cancelledCount = seats.size();

		userShowLimit.setSeatCount(
				Math.max(0, userShowLimit.getSeatCount() - cancelledCount)
		);

		userShowLimitRepository.save(userShowLimit);

		reservation.setStatus("cancelled");
		reservation.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

		reservationRepository.save(reservation);

		log.info(
			"event=reservation_cancelled reservationId={} showId={} userId={} releasedSeats={}",
			reservationId,
			reservation.getShowId(),
			userId,
			seats.stream()
				.map(Seat::getSeatNumber)
				.toList()
		);
	}
	
	private String calculateBodyHash(List<String> seats) {
		List<String> normalizedSeats = seats.stream()
				.sorted()
				.toList();

		String canonical = String.join(",", normalizedSeats);

		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(
					canonical.getBytes(StandardCharsets.UTF_8)
			);

			return HexFormat.of().formatHex(hash);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 not available", e);
		}
	}
	
	private ReserveResponse buildResponse(Reservation reservation) {

		List<ReservationResult> results =
			reservationResultRepository.findByReservationId(reservation.getId());

		List<String> confirmedSeats = results.stream()
			.filter(r -> "confirmed".equals(r.getResult()))
			.map(ReservationResult::getSeatNumber)
			.toList();

		List<DeclinedSeat> declinedSeats = results.stream()
			.filter(r -> "declined".equals(r.getResult()))
			.map(r -> new DeclinedSeat(
				r.getSeatNumber(),
				r.getReason()
			))
			.toList();

		String status = declinedSeats.isEmpty()
			? "confirmed"
			: "partially_confirmed";

		return new ReserveResponse(
			reservation.getId(),
			reservation.getShowId(),
			reservation.getUserId(),
			confirmedSeats,
			declinedSeats,
			reservation.getAmountPaise(),
			status
		);
	}
}