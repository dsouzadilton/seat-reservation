CREATE TABLE shows (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price_paise BIGINT NOT NULL CHECK (price_paise >= 0),
    per_user_limit INTEGER NOT NULL DEFAULT 4 CHECK (per_user_limit > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE seats (
    id BIGSERIAL PRIMARY KEY,
    show_id BIGINT NOT NULL REFERENCES shows(id),
    seat_number VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'available',

    CONSTRAINT uq_seat_show_number UNIQUE (show_id, seat_number),
    CONSTRAINT chk_seat_status
        CHECK (status IN ('available', 'held', 'confirmed'))
);

CREATE TABLE reservations (
    id UUID PRIMARY KEY,
    show_id BIGINT NOT NULL REFERENCES shows(id),
    user_id VARCHAR(255) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    body_hash VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    amount_paise BIGINT NOT NULL CHECK (amount_paise >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_reservation_idempotency
        UNIQUE (show_id, user_id, idempotency_key),

    CONSTRAINT chk_reservation_status
        CHECK (status IN ('confirmed', 'cancelled'))
);

CREATE TABLE reservation_seats (
    reservation_id UUID NOT NULL REFERENCES reservations(id),
    seat_id BIGINT NOT NULL REFERENCES seats(id),

    PRIMARY KEY (reservation_id, seat_id)
);

CREATE TABLE user_show_limits (
    show_id BIGINT NOT NULL REFERENCES shows(id),
    user_id VARCHAR(255) NOT NULL,
    seat_count INTEGER NOT NULL DEFAULT 0 CHECK (seat_count >= 0),

    PRIMARY KEY (show_id, user_id)
);

CREATE INDEX idx_seats_show_status
    ON seats (show_id, status);

CREATE INDEX idx_reservations_show_user
    ON reservations (show_id, user_id);

CREATE INDEX idx_reservation_seats_seat
    ON reservation_seats (seat_id);