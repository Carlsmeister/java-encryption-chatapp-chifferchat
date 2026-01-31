CREATE TABLE group_memberships
(
    id        BIGSERIAL PRIMARY KEY,
    user_id   BIGINT      NOT NULL,
    group_id  BIGINT      NOT NULL,
    role      VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_group_memberships_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_group_memberships_group FOREIGN KEY (group_id) REFERENCES groups (id) ON DELETE CASCADE,
    CONSTRAINT uq_group_memberships_user_group UNIQUE (user_id, group_id)
);

CREATE INDEX idx_group_memberships_user ON group_memberships (user_id);
CREATE INDEX idx_group_memberships_group ON group_memberships (group_id);
