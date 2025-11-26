package se.mau.chifferchat.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for creating message bubbles with proper styling and alignment.
 */
public class MessageBubble {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Creates a message bubble container for the messages VBox.
     *
     * @param text        The message text
     * @param time        The message timestamp
     * @param type        The message type (SENT, RECEIVED, SYSTEM)
     * @param sender      The sender's name (for received messages in group chats)
     * @param isGroupChat Whether this is a group chat
     * @return An HBox containing the message bubble with proper alignment
     */
    public static HBox createMessageBubble(String text, LocalDateTime time, MessageType type,
                                           String sender, boolean isGroupChat) {
        HBox container = new HBox(8);
        container.getStyleClass().add("message-container");
        container.setAlignment(type == MessageType.SENT ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        container.setPadding(new Insets(4, 0, 4, 0));

        if (type == MessageType.SYSTEM) {
            Label systemLabel = new Label(text);
            systemLabel.getStyleClass().add("system-message");
            systemLabel.setWrapText(true);
            systemLabel.setMaxWidth(500);
            container.getChildren().add(systemLabel);
            container.setAlignment(Pos.CENTER);
            return container;
        }

        // Create spacers for alignment
        Region leftSpacer = new Region();
        Region rightSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);
        HBox.setHgrow(rightSpacer, Priority.ALWAYS);

        // Message bubble
        VBox bubble = new VBox(4);
        bubble.getStyleClass().addAll("message-bubble",
                type == MessageType.SENT ? "message-bubble-sent" : "message-bubble-received");
        bubble.setPadding(new Insets(10, 16, 10, 16));
        bubble.setMaxWidth(600);

        // Show sender name for received messages in group chats
        if (type == MessageType.RECEIVED && isGroupChat && sender != null && !sender.isEmpty()) {
            Label senderLabel = new Label(sender);
            senderLabel.getStyleClass().add("message-sender");
            bubble.getChildren().add(senderLabel);
        }

        // Message text
        Label messageLabel = new Label(text);
        messageLabel.getStyleClass().add("message-text");
        messageLabel.setWrapText(true);
        bubble.getChildren().add(messageLabel);

        // Timestamp
        Label timeLabel = new Label(time.format(TIME_FORMAT));
        timeLabel.getStyleClass().add("message-time-external");

        // Add components based on message type
        if (type == MessageType.SENT) {
            // Sent: [spacer] [time] [bubble]
            container.getChildren().addAll(leftSpacer, timeLabel, bubble);
        } else {
            // Received: [bubble] [time] [spacer]
            container.getChildren().addAll(bubble, timeLabel, rightSpacer);
        }

        return container;
    }

    /**
     * Creates a timestamp divider for grouping messages by time.
     *
     * @param text The divider text (e.g., "Today, 9:30 AM")
     * @return An HBox containing the timestamp divider
     */
    public static HBox createTimestampDivider(String text) {
        HBox container = new HBox();
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(16, 0, 8, 0));

        Label label = new Label(text);
        label.getStyleClass().add("message-timestamp-divider");

        container.getChildren().add(label);
        return container;
    }

    /**
     * Formats a LocalDateTime into a readable divider format.
     *
     * @param dateTime The datetime to format
     * @return Formatted string like "Today, 9:30 AM"
     */
    public static String formatTimestampDivider(LocalDateTime dateTime) {
        LocalDateTime now = LocalDateTime.now();

        if (dateTime.toLocalDate().equals(now.toLocalDate())) {
            return "Today, " + dateTime.format(DateTimeFormatter.ofPattern("h:mm a"));
        } else if (dateTime.toLocalDate().equals(now.toLocalDate().minusDays(1))) {
            return "Yesterday, " + dateTime.format(DateTimeFormatter.ofPattern("h:mm a"));
        } else {
            return dateTime.format(DateTimeFormatter.ofPattern("MMM dd, h:mm a"));
        }
    }

    public enum MessageType {
        SENT,
        RECEIVED,
        SYSTEM
    }
}

