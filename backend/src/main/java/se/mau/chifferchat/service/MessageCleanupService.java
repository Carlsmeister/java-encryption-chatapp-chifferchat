package se.mau.chifferchat.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.mau.chifferchat.model.DeliveryStatus;
import se.mau.chifferchat.repository.MessageRepository;

import java.time.LocalDateTime;

/**
 * Service for cleaning up old delivered messages.
 * Messages marked as DELIVERED and older than retention period are deleted.
 * This keeps database size manageable while preserving undelivered messages.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MessageCleanupService {

    private final MessageRepository messageRepository;

    @Value("${chifferchat.messages.retention-days:60}")
    private int retentionDays;

    /**
     * Scheduled task to clean up old delivered messages.
     * Runs daily at 2 AM.
     * Only deletes messages with status=DELIVERED older than retention period.
     * Undelivered messages are never deleted automatically.
     */
    @Scheduled(cron = "${chifferchat.messages.cleanup-schedule:0 0 2 * * *}")
    @Transactional
    public void cleanupOldDeliveredMessages() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);

        log.info("Starting cleanup of delivered messages older than {} (retention: {} days)",
                cutoffDate, retentionDays);

        try {
            // Delete only DELIVERED messages older than retention period
            long deletedCount = messageRepository.deleteByDeliveryStatusAndTimestampBefore(
                    DeliveryStatus.DELIVERED,
                    cutoffDate
            );

            if (deletedCount > 0) {
                log.info("Cleaned up {} old delivered messages", deletedCount);
            } else {
                log.debug("No old delivered messages to clean up");
            }
        } catch (Exception e) {
            log.error("Failed to clean up old messages: {}", e.getMessage(), e);
        }
    }
}

