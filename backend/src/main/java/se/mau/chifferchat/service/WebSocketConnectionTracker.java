package se.mau.chifferchat.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Thread-safe service for tracking active WebSocket connections.
 * Supports multiple concurrent sessions per user (multi-device).
 * <p>
 * Used by OfflineMessageService to determine if user is online.
 */
@Service
@Slf4j
public class WebSocketConnectionTracker {

    // Map: username -> Set of sessionIds
    // Thread-safe for concurrent WebSocket connections
    private final ConcurrentHashMap<String, Set<String>> connectedUsers = new ConcurrentHashMap<>();

    /**
     * Register a new WebSocket connection for a user.
     * Supports multiple devices per user.
     *
     * @param username  User's username
     * @param sessionId Unique WebSocket session ID
     */
    public void addConnection(String username, String sessionId) {
        connectedUsers.computeIfAbsent(username, k -> new CopyOnWriteArraySet<>())
                .add(sessionId);

        log.debug("Connection added: {} (session: {}), total sessions: {}",
                username, sessionId, connectedUsers.get(username).size());
    }

    /**
     * Unregister a WebSocket connection.
     * If last session for user, removes user from map.
     *
     * @param username  User's username
     * @param sessionId WebSocket session ID to remove
     */
    public void removeConnection(String username, String sessionId) {
        Set<String> sessions = connectedUsers.get(username);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                connectedUsers.remove(username);
                log.debug("All connections closed for user: {}", username);
            } else {
                log.debug("Connection removed: {} (session: {}), remaining: {}",
                        username, sessionId, sessions.size());
            }
        }
    }

    /**
     * Check if user has any active WebSocket connections.
     * Critical for offline message queue logic.
     *
     * @param username User to check
     * @return true if user has at least one active connection
     */
    public boolean isUserConnected(String username) {
        Set<String> sessions = connectedUsers.get(username);
        return sessions != null && !sessions.isEmpty();
    }

    /**
     * Get all active session IDs for a user.
     * Used for broadcasting to all user's devices.
     *
     * @param username User's username
     * @return Set of session IDs, empty if offline
     */
    public Set<String> getConnectedSessions(String username) {
        return connectedUsers.getOrDefault(username, Set.of());
    }

    /**
     * Count active connections for a user.
     * Useful for debugging and monitoring.
     */
    public int getConnectionCount(String username) {
        return getConnectedSessions(username).size();
    }

    /**
     * Get total number of connected users (not sessions).
     * For monitoring/metrics.
     */
    public int getTotalConnectedUsers() {
        return connectedUsers.size();
    }
}

