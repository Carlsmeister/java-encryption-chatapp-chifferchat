package se.mau.chifferchat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import se.mau.chifferchat.security.WebSocketAuthInterceptor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * WebSocket configuration using Spring's simple in-memory broker.
 * <p>
 * Configures STOMP over SockJS for real-time bidirectional communication.
 * Enables encrypted message delivery for direct and group chats.
 * <p>
 * SCALABILITY NOTE:
 * This configuration uses an in-memory broker suitable for single-instance deployments.
 * For horizontal scaling (multiple backend instances), migrate to an external message broker:
 * <p>
 * Option 1: RabbitMQ (STOMP-native)
 * - Add dependency: spring-boot-starter-amqp
 * - Replace enableSimpleBroker() with:
 * config.enableStompBrokerRelay("/topic", "/queue")
 * .setRelayHost("rabbitmq.example.com")
 * .setRelayPort(61613);
 * <p>
 * Option 2: Redis Pub/Sub
 * - Add dependency: spring-boot-starter-data-redis
 * - Use custom message channel with Redis backend
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final WebSocketAuthInterceptor webSocketAuthInterceptor;
    private final List<String> allowedOrigins;

    public WebSocketConfig(
            WebSocketAuthInterceptor webSocketAuthInterceptor,
            @Value("${cors.allowed-origins:}") String allowedOrigins
    ) {
        this.webSocketAuthInterceptor = webSocketAuthInterceptor;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Configure message broker destinations.
     * - /topic: for broadcasting to multiple subscribers (e.g., group messages, presence)
     * - /queue: for point-to-point messages (e.g., direct messages)
     * - /app: prefix for client-to-server messages
     * - /user: prefix for user-specific destinations
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    /**
     * Register STOMP endpoints for WebSocket connections.
     * Endpoint: /ws
     * Fallback: SockJS for browsers that don't support WebSocket
     * CORS: Configured based on application properties
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] originsArray = allowedOrigins.isEmpty()
                ? new String[]{"*"}
                : allowedOrigins.toArray(new String[0]);

        registry.addEndpoint("/ws")
                .setAllowedOrigins(originsArray)
                .withSockJS();
    }

    /**
     * Configure client inbound channel with JWT authentication interceptor.
     * This validates JWT tokens in STOMP CONNECT frames.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor);
    }
}

