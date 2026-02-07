package se.mau.chifferchat.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import se.mau.chifferchat.model.DeliveryStatus;
import se.mau.chifferchat.model.Message;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    @Query("SELECT m FROM Message m WHERE m.recipient.id = :recipientId ORDER BY m.timestamp DESC")
    Page<Message> findByRecipientIdOrderByTimestampDesc(
            @Param("recipientId") Long recipientId,
            Pageable pageable
    );

    @Query("SELECT m FROM Message m WHERE m.group.id = :groupId ORDER BY m.timestamp DESC")
    Page<Message> findByGroupIdOrderByTimestampDesc(
            @Param("groupId") Long groupId,
            Pageable pageable
    );

    @Query(
            "SELECT m FROM Message m "
                    + "WHERE ((m.sender.id = :userId1 AND m.recipient.id = :userId2) "
                    + "OR (m.sender.id = :userId2 AND m.recipient.id = :userId1)) "
                    + "AND m.group IS NULL "
                    + "ORDER BY m.timestamp DESC"
    )
    Page<Message> findConversationHistory(
            @Param("userId1") Long userId1,
            @Param("userId2") Long userId2,
            Pageable pageable
    );

    /**
     * Find all undelivered messages for a recipient.
     * Returns messages with delivery_status != DELIVERED in chronological order.
     *
     * @param recipientId Recipient user ID
     * @return List of undelivered messages ordered by timestamp ASC
     */
    @Query("SELECT m FROM Message m WHERE m.recipient.id = :recipientId " +
            "AND m.deliveryStatus != :deliveredStatus " +
            "ORDER BY m.timestamp ASC")
    List<Message> findUndeliveredMessagesByRecipient(
            @Param("recipientId") Long recipientId,
            @Param("deliveredStatus") DeliveryStatus deliveredStatus
    );

    /**
     * Find all undelivered messages for a recipient (convenience method).
     */
    default List<Message> findUndeliveredMessagesByRecipient(Long recipientId) {
        return findUndeliveredMessagesByRecipient(recipientId, DeliveryStatus.DELIVERED);
    }

    /**
     * Delete old delivered messages for cleanup.
     * Only deletes messages with the specified status older than the cutoff timestamp.
     *
     * @param status    Delivery status to filter by
     * @param timestamp Delete messages older than this timestamp
     * @return Number of deleted messages
     */
    @Modifying
    @Query("DELETE FROM Message m WHERE m.deliveryStatus = :status AND m.timestamp < :timestamp")
    long deleteByDeliveryStatusAndTimestampBefore(
            @Param("status") DeliveryStatus status,
            @Param("timestamp") LocalDateTime timestamp
    );
}
