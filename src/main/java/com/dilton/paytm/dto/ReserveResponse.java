package com.dilton.paytm.dto;

import java.util.List;
import java.util.UUID;

public record ReserveResponse(
        UUID reservationId,
        Long showId,
        String userId,
        List<String> confirmedSeats,
        List<DeclinedSeat> declinedSeats,
        Long amountPaise,
        String status
) {
}