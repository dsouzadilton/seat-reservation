CREATE TABLE reservation_results (
    reservation_id UUID NOT NULL,
    seat_number VARCHAR(50) NOT NULL,
    result VARCHAR(20) NOT NULL,
    reason VARCHAR(50),

    CONSTRAINT fk_reservation_results_reservation
        FOREIGN KEY (reservation_id)
        REFERENCES reservations(id),

    CONSTRAINT chk_reservation_results_result
        CHECK (result IN ('confirmed', 'declined')),

    PRIMARY KEY (reservation_id, seat_number)
);