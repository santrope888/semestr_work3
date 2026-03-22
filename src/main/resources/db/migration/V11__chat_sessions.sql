CREATE TABLE chat_sessions (
                               session_id BIGSERIAL PRIMARY KEY,
                               title VARCHAR(255),
                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE
);