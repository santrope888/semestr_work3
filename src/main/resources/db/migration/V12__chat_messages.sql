CREATE TABLE chat_messages (
                               message_id BIGSERIAL PRIMARY KEY,
                               role VARCHAR(20) NOT NULL,
                               content TEXT NOT NULL,
                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               session_id BIGINT NOT NULL REFERENCES chat_sessions(session_id) ON DELETE CASCADE
);