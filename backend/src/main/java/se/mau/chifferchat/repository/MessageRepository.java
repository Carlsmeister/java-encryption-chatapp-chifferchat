package se.mau.chifferchat.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import se.mau.chifferchat.model.Message;

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
}
