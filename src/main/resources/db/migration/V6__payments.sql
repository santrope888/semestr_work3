CREATE TABLE payments (
                          payment_id BIGSERIAL PRIMARY KEY,
                          amount INT NOT NULL,
                          currency VARCHAR(10) NOT NULL DEFAULT 'RUB',
                          status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                          method VARCHAR(50),
                          paid_at TIMESTAMP,
                          booking_id BIGINT NOT NULL UNIQUE REFERENCES bookings(booking_id)
);