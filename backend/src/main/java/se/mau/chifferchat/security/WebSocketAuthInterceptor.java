package se.mau.chifferchat.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * WebSocket authentication interceptor for STOMP connections.
 * <p>
 * Validates JWT tokens provided in the STOMP CONNECT frame header.
 * Sets the authenticated user principal for subsequent WebSocket messages.
 * <p>
 * Authentication Flow:
 * 1. Client connects with Authorization header: "Bearer {JWT}"
 * 2. Extract and validate JWT token
 * 3. Set user principal in STOMP session
 * 4. Reject connection if token is invalid or missing
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Intercept messages before they are sent to the channel.
     * Validates JWT token for CONNECT commands.
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("WebSocket connection attempt without valid Authorization header");
                throw new IllegalArgumentException("Missing or invalid Authorization header");
            }

            String token = authHeader.substring(7);

            try {
                if (jwtTokenProvider.validateToken(token)) {
                    String username = jwtTokenProvider.getUsernameFromToken(token);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(username, null, Collections.emptyList());

                    accessor.setUser(authentication);
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.debug("WebSocket authenticated for user: {}", username);
                } else {
                    log.warn("Invalid JWT token in WebSocket connection");
                    throw new IllegalArgumentException("Invalid JWT token");
                }
            } catch (Exception e) {
                log.error("WebSocket authentication failed: {}", e.getMessage());
                throw new IllegalArgumentException("JWT token validation failed: " + e.getMessage());
            }
        }

        return message;
    }
}


