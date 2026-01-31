CREATE TABLE refresh_tokens
(
    id          BIGSERIAL PRIMARY KEY,
    token       UUID      NOT NULL UNIQUE,
    user_id     BIGINT    NOT NULL,
    expiry_date TIMESTAMP NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_expiry ON refresh_tokens (expiry_date);
