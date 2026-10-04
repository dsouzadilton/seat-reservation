package com.dilton.paytm.service;

import com.dilton.paytm.entity.Show;
import com.dilton.paytm.repository.SeatRepository;
import com.dilton.paytm.repository.ShowRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MetricsRefreshService {

    private final SeatRepository seatRepository;
    private final ShowRepository showRepository;
    private final ReservationMetrics reservationMetrics;

    public MetricsRefreshService(
            SeatRepository seatRepository,
            ShowRepository showRepository,
            ReservationMetrics reservationMetrics
    ) {
        this.seatRepository = seatRepository;
        this.showRepository = showRepository;
        this.reservationMetrics = reservationMetrics;
    }

    @Scheduled(fixedDelay = 1000)
    public void refreshAvailableSeats() {

        for (Show show : showRepository.findAll()) {

            long available =
                    seatRepository.countAvailableSeatsByShowId(show.getId());

            reservationMetrics.setSeatsAvailable(
                    show.getId(),
                    available
            );
        }
    }
}