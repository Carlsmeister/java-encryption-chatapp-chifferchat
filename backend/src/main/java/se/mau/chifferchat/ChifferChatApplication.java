package se.mau.chifferchat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the ChifferChat Spring Boot application.
 * <p>
 * This application provides an end-to-end encrypted chat platform with:
 * - REST API for user/group/message management
 * - WebSocket (STOMP) for real-time messaging
 * - JWT-based authentication
 * - Client-side encryption (server cannot decrypt messages)
 */
@SpringBootApplication
public class ChifferChatApplication {
    static void main(String[] args) {
        SpringApplication.run(ChifferChatApplication.class, args);
    }
}
