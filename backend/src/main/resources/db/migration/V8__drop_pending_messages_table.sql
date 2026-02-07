-- Drop pending_messages table (replaced by delivery_status on messages table)
-- The new unified delivery system tracks status directly on messages

DROP INDEX IF EXISTS idx_pending_messages_retry;
DROP INDEX IF EXISTS idx_pending_messages_created;
DROP INDEX IF EXISTS idx_pending_messages_expires;
DROP INDEX IF EXISTS idx_pending_messages_recipient;

DROP TABLE IF EXISTS pending_messages;

-- Note: Messages table now has delivery_status column (added in V7)
-- No data migration needed - existing pending messages will be delivered on next user login

