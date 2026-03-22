CREATE TABLE reviews (
                         review_id BIGSERIAL PRIMARY KEY,
                         rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
                         comment TEXT,
                         created_at DATE NOT NULL DEFAULT CURRENT_DATE,
                         user_id BIGINT NOT NULL REFERENCES users(user_id),
                         car_id BIGINT NOT NULL REFERENCES cars(car_id)
);