CREATE TABLE groups
(
    id         BIGSERIAL PRIMARY KEY,
    group_id   UUID         NOT NULL UNIQUE,
    group_name VARCHAR(100) NOT NULL,
    creator_id BIGINT       NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_groups_creator FOREIGN KEY (creator_id) REFERENCES users (id)
);

CREATE INDEX idx_groups_group_id ON groups (group_id);
CREATE INDEX idx_groups_creator ON groups (creator_id);
