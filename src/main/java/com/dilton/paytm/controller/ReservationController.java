package com.dilton.paytm.controller;

import com.dilton.paytm.config.AuthFilter;
import com.dilton.paytm.dto.ReserveRequest;
import com.dilton.paytm.dto.ReserveResponse;
import com.dilton.paytm.service.ReservationService;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping("/shows/{showId}/reserve")
    public ResponseEntity<ReserveResponse> reserve(
            @PathVariable Long showId,
            @RequestBody ReserveRequest request,
            HttpServletRequest httpRequest
    ) {
        String userId = (String) httpRequest.getAttribute(
                AuthFilter.USER_ID_ATTRIBUTE
        );

        ReserveResponse response = reservationService.reserve(
                showId,
                userId,
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
	
	@PostMapping("/reservations/{reservationId}/cancel")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void cancel(
			@PathVariable UUID reservationId,
			HttpServletRequest httpRequest
	) {
		String userId = (String) httpRequest.getAttribute(
				AuthFilter.USER_ID_ATTRIBUTE
		);

		reservationService.cancel(reservationId, userId);
	}
}