CREATE TABLE users
(
    id             BIGSERIAL PRIMARY KEY,
    username       VARCHAR(50) UNIQUE  NOT NULL,
    email          VARCHAR(100) UNIQUE NOT NULL,
    password_hash  VARCHAR(255)        NOT NULL,
    public_key_pem TEXT                NOT NULL,
    created_at     TIMESTAMP           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen      TIMESTAMP,
    is_active      BOOLEAN             NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_users_username ON users (username);
CREATE INDEX idx_users_email ON users (email);
