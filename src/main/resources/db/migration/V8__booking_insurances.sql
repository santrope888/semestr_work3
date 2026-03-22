CREATE TABLE IF NOT EXISTS insurances (
                                    booking_id BIGINT NOT NULL REFERENCES bookings(booking_id) ON DELETE CASCADE,
                                    insurance_id BIGINT NOT NULL REFERENCES insurances(insurance_id) ON DELETE CASCADE,
                                    PRIMARY KEY (booking_id, insurance_id)
);