package com.dilton.paytm.service;

import com.dilton.paytm.dto.ShowResponse;
import com.dilton.paytm.dto.ShowSeatResponse;
import com.dilton.paytm.entity.Seat;
import com.dilton.paytm.entity.Show;
import com.dilton.paytm.repository.SeatRepository;
import com.dilton.paytm.repository.ShowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class ShowService {

    private final ShowRepository showRepository;
    private final SeatRepository seatRepository;

    public ShowService(
            ShowRepository showRepository,
            SeatRepository seatRepository
    ) {
        this.showRepository = showRepository;
        this.seatRepository = seatRepository;
    }

    @Transactional
    public Show createShow(
            String name,
            List<String> seatNumbers,
            Long pricePaise
    ) {
        Show show = new Show();
        show.setName(name);
        show.setPricePaise(pricePaise);
        show.setPerUserLimit(4);
        show.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));

        show = showRepository.save(show);

        for (String seatNumber : seatNumbers) {
            Seat seat = new Seat();
            seat.setShowId(show.getId());
            seat.setSeatNumber(seatNumber);
            seat.setStatus("available");

            seatRepository.save(seat);
        }

        return show;
    }
	
	@Transactional(readOnly = true)
	public ShowResponse getShow(Long showId) {

		Show show = showRepository.findById(showId)
				.orElseThrow(() -> new IllegalArgumentException("Show not found"));

		List<Seat> seats =
				seatRepository.findByShowIdOrderBySeatNumber(showId);

		List<ShowSeatResponse> seatResponses = seats.stream()
				.map(seat -> new ShowSeatResponse(
						seat.getSeatNumber(),
						seat.getStatus()
				))
				.toList();

		long available = seats.stream()
				.filter(s -> "available".equals(s.getStatus()))
				.count();

		long held = seats.stream()
				.filter(s -> "held".equals(s.getStatus()))
				.count();

		long confirmed = seats.stream()
				.filter(s -> "confirmed".equals(s.getStatus()))
				.count();

		return new ShowResponse(
				show.getId(),
				show.getName(),
				show.getPricePaise(),
				show.getPerUserLimit(),
				seatResponses,
				available,
				held,
				confirmed,
				seats.size()
		);
	}
}