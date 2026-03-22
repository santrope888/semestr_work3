CREATE TABLE cars (
                      car_id BIGSERIAL PRIMARY KEY,
                      brand VARCHAR(255) NOT NULL,
                      model VARCHAR(255) NOT NULL,
                      year INT NOT NULL,
                      color VARCHAR(100) NOT NULL,
                      price_per_day INT NOT NULL,
                      seats INT NOT NULL DEFAULT 5,
                      transmission VARCHAR(50) NOT NULL DEFAULT 'Automatic',
                      engine VARCHAR(100) NOT NULL DEFAULT 'Petrol',
                      drive VARCHAR(50) NOT NULL DEFAULT 'FWD',
                      image_path VARCHAR(500),
                      description TEXT,
                      available BOOLEAN NOT NULL DEFAULT TRUE,
                      created_at DATE NOT NULL DEFAULT CURRENT_DATE,
                      category_id BIGINT REFERENCES categories(category_id)
);