package com.dilton.paytm.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "reservation_results")
@IdClass(ReservationResult.ReservationResultId.class)
public class ReservationResult {

    @Id
    @Column(name = "reservation_id", nullable = false)
    private UUID reservationId;

    @Id
    @Column(name = "seat_number", nullable = false, length = 50)
    private String seatNumber;

    @Column(nullable = false, length = 20)
    private String result;

    @Column(length = 50)
    private String reason;

    public UUID getReservationId() {
        return reservationId;
    }

    public void setReservationId(UUID reservationId) {
        this.reservationId = reservationId;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public static class ReservationResultId implements Serializable {

        private UUID reservationId;
        private String seatNumber;

        public ReservationResultId() {
        }

        public ReservationResultId(UUID reservationId, String seatNumber) {
            this.reservationId = reservationId;
            this.seatNumber = seatNumber;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ReservationResultId that)) return false;
            return Objects.equals(reservationId, that.reservationId)
                    && Objects.equals(seatNumber, that.seatNumber);
        }

        @Override
        public int hashCode() {
            return Objects.hash(reservationId, seatNumber);
        }
    }
}