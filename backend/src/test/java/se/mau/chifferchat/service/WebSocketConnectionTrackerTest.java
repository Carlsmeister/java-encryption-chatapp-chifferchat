package se.mau.chifferchat.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for WebSocketConnectionTracker.
 */
class WebSocketConnectionTrackerTest {

    private WebSocketConnectionTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new WebSocketConnectionTracker();
    }

    @Test
    @DisplayName("Should track single connection")
    void shouldTrackSingleConnection() {
        // When
        tracker.addConnection("alice", "session1");

        // Then
        assertThat(tracker.isUserConnected("alice")).isTrue();
        assertThat(tracker.getConnectionCount("alice")).isEqualTo(1);
        assertThat(tracker.getTotalConnectedUsers()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should track multiple connections for same user (multi-device)")
    void shouldTrackMultipleConnectionsForSameUser() {
        // When
        tracker.addConnection("alice", "session1");
        tracker.addConnection("alice", "session2");
        tracker.addConnection("alice", "session3");

        // Then
        assertThat(tracker.isUserConnected("alice")).isTrue();
        assertThat(tracker.getConnectionCount("alice")).isEqualTo(3);
        assertThat(tracker.getTotalConnectedUsers()).isEqualTo(1);

        Set<String> sessions = tracker.getConnectedSessions("alice");
        assertThat(sessions).containsExactlyInAnyOrder("session1", "session2", "session3");
    }

    @Test
    @DisplayName("Should track multiple users")
    void shouldTrackMultipleUsers() {
        // When
        tracker.addConnection("alice", "session1");
        tracker.addConnection("bob", "session2");
        tracker.addConnection("charlie", "session3");

        // Then
        assertThat(tracker.isUserConnected("alice")).isTrue();
        assertThat(tracker.isUserConnected("bob")).isTrue();
        assertThat(tracker.isUserConnected("charlie")).isTrue();
        assertThat(tracker.getTotalConnectedUsers()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should remove single connection")
    void shouldRemoveSingleConnection() {
        // Given
        tracker.addConnection("alice", "session1");

        // When
        tracker.removeConnection("alice", "session1");

        // Then
        assertThat(tracker.isUserConnected("alice")).isFalse();
        assertThat(tracker.getConnectionCount("alice")).isEqualTo(0);
        assertThat(tracker.getTotalConnectedUsers()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should keep user connected when removing one of multiple sessions")
    void shouldKeepUserConnectedWhenRemovingOneSession() {
        // Given
        tracker.addConnection("alice", "session1");
        tracker.addConnection("alice", "session2");
        tracker.addConnection("alice", "session3");

        // When
        tracker.removeConnection("alice", "session1");

        // Then
        assertThat(tracker.isUserConnected("alice")).isTrue();
        assertThat(tracker.getConnectionCount("alice")).isEqualTo(2);

        Set<String> sessions = tracker.getConnectedSessions("alice");
        assertThat(sessions).containsExactlyInAnyOrder("session2", "session3");
    }

    @Test
    @DisplayName("Should mark user offline only after removing all sessions")
    void shouldMarkUserOfflineAfterRemovingAllSessions() {
        // Given
        tracker.addConnection("alice", "session1");
        tracker.addConnection("alice", "session2");

        // When
        tracker.removeConnection("alice", "session1");

        // Then - Still connected (has session2)
        assertThat(tracker.isUserConnected("alice")).isTrue();

        // When - Remove last session
        tracker.removeConnection("alice", "session2");

        // Then - Now offline
        assertThat(tracker.isUserConnected("alice")).isFalse();
        assertThat(tracker.getTotalConnectedUsers()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should handle removing non-existent connection gracefully")
    void shouldHandleRemovingNonExistentConnection() {
        // When
        tracker.removeConnection("alice", "session1");

        // Then - Should not throw exception
        assertThat(tracker.isUserConnected("alice")).isFalse();
    }

    @Test
    @DisplayName("Should return empty set for offline user sessions")
    void shouldReturnEmptySetForOfflineUser() {
        // When
        Set<String> sessions = tracker.getConnectedSessions("alice");

        // Then
        assertThat(sessions).isEmpty();
    }

    @Test
    @DisplayName("Should return zero count for offline user")
    void shouldReturnZeroCountForOfflineUser() {
        // When
        int count = tracker.getConnectionCount("alice");

        // Then
        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("Should handle concurrent connections (thread-safety)")
    void shouldHandleConcurrentConnections() throws InterruptedException {
        // Given
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];

        // When - Add connections concurrently
        for (int i = 0; i < threadCount; i++) {
            final int sessionNum = i;
            threads[i] = new Thread(() -> {
                tracker.addConnection("alice", "session" + sessionNum);
            });
            threads[i].start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Then
        assertThat(tracker.isUserConnected("alice")).isTrue();
        assertThat(tracker.getConnectionCount("alice")).isEqualTo(threadCount);
    }

    @Test
    @DisplayName("Should prevent duplicate session IDs")
    void shouldPreventDuplicateSessionIds() {
        // When
        tracker.addConnection("alice", "session1");
        tracker.addConnection("alice", "session1");  // Duplicate
        tracker.addConnection("alice", "session1");  // Duplicate

        // Then - Should only count once
        assertThat(tracker.getConnectionCount("alice")).isEqualTo(1);
    }
}

