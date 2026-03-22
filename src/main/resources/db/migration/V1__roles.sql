CREATE TABLE roles (
                       role_id BIGSERIAL PRIMARY KEY,
                       name VARCHAR(50) NOT NULL UNIQUE
);

INSERT INTO roles (name) VALUES ('USER'), ('ADMIN');