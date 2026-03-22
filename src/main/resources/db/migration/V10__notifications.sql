CREATE TABLE notifications (
                               notification_id BIGSERIAL PRIMARY KEY,
                               message TEXT NOT NULL,
                               type VARCHAR(50) NOT NULL,
                               is_read BOOLEAN NOT NULL DEFAULT FALSE,
                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               user_id BIGINT NOT NULL REFERENCES users(user_id) ON DELETE CASCADE
);