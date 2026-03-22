CREATE TABLE categories (
                            category_id BIGSERIAL PRIMARY KEY,
                            name VARCHAR(100) NOT NULL UNIQUE,
                            description TEXT
);

INSERT INTO categories (name, description) VALUES
                                               ('Эконом', 'Бюджетные автомобили для повседневных поездок'),
                                               ('Комфорт', 'Автомобили среднего класса с хорошим оснащением'),
                                               ('Бизнес', 'Премиальные автомобили для деловых поездок'),
                                               ('Внедорожник', 'Полноприводные автомобили для загородных поездок');