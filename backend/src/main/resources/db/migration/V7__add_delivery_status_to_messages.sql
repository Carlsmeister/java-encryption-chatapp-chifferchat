-- Add delivery status tracking to messages table
-- Supports: SENDING (in transit), SENT (delivered to WebSocket), DELIVERED (acknowledged by client)

-- Add delivery_status column with enum type
ALTER TABLE messages
    ADD COLUMN delivery_status VARCHAR(20) NOT NULL DEFAULT 'SENDING';

-- Add delivered_at timestamp column
ALTER TABLE messages
    ADD COLUMN delivered_at TIMESTAMP NULL;

-- Add constraint to ensure valid delivery status values
ALTER TABLE messages
    ADD CONSTRAINT chk_delivery_status CHECK (
        delivery_status IN ('SENDING', 'SENT', 'DELIVERED')
        );

-- Create composite index for efficient queries on undelivered messages
CREATE INDEX idx_messages_delivery_status ON messages (recipient_id, delivery_status, timestamp);

-- Migrate existing messages to DELIVERED status (backwards compatibility)
-- Assumes all existing messages were successfully delivered
UPDATE messages
SET delivery_status = 'DELIVERED',
    delivered_at    = timestamp
WHERE delivery_status = 'SENDING';

-- Add comments for documentation
COMMENT
ON COLUMN messages.delivery_status IS 'Message delivery state: SENDING (in transit), SENT (delivered to WebSocket), DELIVERED (acknowledged by client)';
COMMENT
ON COLUMN messages.delivered_at IS 'Timestamp when message was acknowledged by recipient client';

