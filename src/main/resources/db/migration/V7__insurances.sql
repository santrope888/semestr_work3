CREATE TABLE insurances (
                            insurance_id BIGSERIAL PRIMARY KEY,
                            name VARCHAR(255) NOT NULL UNIQUE,
                            description TEXT,
                            price_per_day INT NOT NULL
);

CREATE TABLE booking_insurances (
                                    booking_id BIGINT NOT NULL REFERENCES bookings(booking_id) ON DELETE CASCADE,
                                    insurance_id BIGINT NOT NULL REFERENCES insurances(insurance_id) ON DELETE CASCADE,
                                    PRIMARY KEY (booking_id, insurance_id)
);

INSERT INTO insurances (name, description, price_per_day) VALUES
                                                              ('Базовая (ОСАГО)', 'Покрывает ущерб, причинённый третьим лицам при ДТП', 350),
                                                              ('От угона', 'Страхование от угона и хищения автомобиля', 800),
                                                              ('Полное КАСКО', 'Полное покрытие: ДТП, угон, стихийные бедствия, вандализм, падение предметов', 2500),
                                                              ('КАСКО Лайт', 'Частичное КАСКО: только ДТП и стихийные бедствия, без угона', 1400),
                                                              ('Защита от ДТП', 'Покрытие ущерба автомобилю при ДТП по вашей вине', 900),
                                                              ('Страхование пассажиров', 'Покрытие вреда здоровью водителя и пассажиров до 500 000 руб. на человека', 450),
                                                              ('Помощь на дороге', 'Эвакуатор, техпомощь, подвоз топлива, вскрытие замков — 24/7 по всей России', 300);