CREATE TABLE bookings (
                          booking_id BIGSERIAL PRIMARY KEY,
                          start_date DATE NOT NULL,
                          end_date DATE NOT NULL,
                          total_price INT NOT NULL,
                          status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                          created_at DATE NOT NULL DEFAULT CURRENT_DATE,
                          user_id BIGINT NOT NULL REFERENCES users(user_id),
                          car_id BIGINT NOT NULL REFERENCES cars(car_id)
);