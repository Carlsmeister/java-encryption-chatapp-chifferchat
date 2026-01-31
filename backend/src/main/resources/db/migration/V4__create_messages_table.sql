CREATE TABLE messages
(
    id                BIGSERIAL PRIMARY KEY,
    sender_id         BIGINT       NOT NULL,
    recipient_id      BIGINT,
    group_id          BIGINT,
    encrypted_content TEXT         NOT NULL,
    encrypted_aes_key TEXT         NOT NULL,
    iv                VARCHAR(255) NOT NULL,
    message_type      VARCHAR(20)  NOT NULL,
    timestamp         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_messages_sender FOREIGN KEY (sender_id) REFERENCES users (id),
    CONSTRAINT fk_messages_recipient FOREIGN KEY (recipient_id) REFERENCES users (id),
    CONSTRAINT fk_messages_group FOREIGN KEY (group_id) REFERENCES groups (id),
    CONSTRAINT chk_recipient_or_group CHECK (
        (recipient_id IS NOT NULL AND group_id IS NULL) OR
        (recipient_id IS NULL AND group_id IS NOT NULL)
        )
);

CREATE INDEX idx_messages_recipient ON messages (recipient_id);
CREATE INDEX idx_messages_group ON messages (group_id);
CREATE INDEX idx_messages_timestamp ON messages (timestamp);
