package se.mau.chifferchat.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "messages",
        indexes = {
                @Index(name = "idx_messages_recipient", columnList = "recipient_id"),
                @Index(name = "idx_messages_group", columnList = "group_id"),
                @Index(name = "idx_messages_timestamp", columnList = "timestamp"),
                @Index(name = "idx_messages_delivery_status", columnList = "recipient_id, delivery_status, timestamp")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User recipient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Group group;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String encryptedContent;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String encryptedAesKey;

    @Column(nullable = false, length = 255)
    private String iv;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageType messageType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DeliveryStatus deliveryStatus = DeliveryStatus.SENDING;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column
    private LocalDateTime deliveredAt;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
