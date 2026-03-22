CREATE TABLE user_favorites (
                                favorite_id BIGSERIAL PRIMARY KEY,
                                created_at DATE NOT NULL DEFAULT CURRENT_DATE,
                                user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
                                car_id BIGINT NOT NULL REFERENCES cars(car_id) ON DELETE CASCADE,
                                UNIQUE (user_id, car_id)
);