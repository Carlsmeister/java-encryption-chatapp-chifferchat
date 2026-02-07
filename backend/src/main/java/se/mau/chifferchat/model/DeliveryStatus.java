package se.mau.chifferchat.model;

/**
 * Message delivery status.
 * <p>
 * SENDING - Message saved to database but not yet delivered via WebSocket
 * SENT - Message delivered to recipient's WebSocket connection
 * DELIVERED - Message acknowledged by recipient client
 */
public enum DeliveryStatus {
    /**
     * Message persisted but not yet delivered via WebSocket.
     * Used for offline recipients or during network issues.
     */
    SENDING,

    /**
     * Message successfully delivered to recipient's WebSocket connection.
     * Recipient's device has received the message.
     * Awaiting client acknowledgment.
     */
    SENT,

    /**
     * Client has acknowledged receipt.
     * Message successfully processed by recipient's app.
     * Final state (no read receipts).
     */
    DELIVERED
}

