CREATE TABLE users (
                       user_id BIGSERIAL PRIMARY KEY,
                       username VARCHAR(255) NOT NULL UNIQUE,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       phone_number VARCHAR(50),
                       avatar_path VARCHAR(500),
                       created_at DATE NOT NULL DEFAULT CURRENT_DATE,
                       role_id BIGINT REFERENCES roles(role_id)
);