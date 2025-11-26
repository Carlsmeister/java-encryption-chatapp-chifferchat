package se.mau.chifferchat.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

/**
 * Custom cell component for chat list items.
 * Displays user/group avatar, name, preview message, and presence indicator.
 */
public class ChatListItem extends HBox {

    private final String name;
    private final String preview;
    private final boolean isOnline;
    private final boolean isGroup;
    private final int unreadCount;
    private final StackPane avatarContainer;
    private final Label nameLabel;
    private final Label previewLabel;
    private final Label timeLabel;
    private final Label unreadLabel;

    public ChatListItem(String name, String preview, boolean isOnline, boolean isGroup, int unreadCount, String time) {
        this.name = name;
        this.preview = preview;
        this.isOnline = isOnline;
        this.isGroup = isGroup;
        this.unreadCount = unreadCount;

        getStyleClass().add("chat-list-item");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(12);
        setPadding(new Insets(8));

        // Avatar with presence indicator
        avatarContainer = createAvatar();

        // Middle section: name and preview
        VBox infoBox = new VBox(4);
        infoBox.getStyleClass().add("chat-item-info");
        infoBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        nameLabel = new Label(name);
        nameLabel.getStyleClass().add("chat-item-name");
        nameLabel.setMaxWidth(Double.MAX_VALUE);

        previewLabel = new Label(preview);
        previewLabel.getStyleClass().add("chat-item-preview");
        previewLabel.setMaxWidth(180);
        previewLabel.setWrapText(false);

        infoBox.getChildren().addAll(nameLabel, previewLabel);

        // Right section: time and unread badge
        VBox rightBox = new VBox(4);
        rightBox.setAlignment(Pos.TOP_RIGHT);

        timeLabel = new Label(time);
        timeLabel.getStyleClass().add("chat-item-time");

        unreadLabel = new Label(String.valueOf(unreadCount));
        unreadLabel.getStyleClass().add("chat-item-unread");
        unreadLabel.setVisible(unreadCount > 0);
        unreadLabel.setManaged(unreadCount > 0);

        rightBox.getChildren().addAll(timeLabel, unreadLabel);

        getChildren().addAll(avatarContainer, infoBox, rightBox);
    }

    private StackPane createAvatar() {
        StackPane stack = new StackPane();
        stack.getStyleClass().add("chat-item-avatar");
        stack.setPrefSize(48, 48);
        stack.setMinSize(48, 48);
        stack.setMaxSize(48, 48);

        // Avatar circle
        Circle circle = new Circle(24);
        circle.getStyleClass().add("chat-avatar-circle");

        // Avatar text (first letter of name)
        String avatarText = isGroup ? "🔒" : name.substring(0, Math.min(1, name.length())).toUpperCase();
        Label avatarLabel = new Label(avatarText);
        avatarLabel.getStyleClass().add("chat-avatar-text");

        stack.getChildren().addAll(circle, avatarLabel);

        // Presence indicator (only for users, not groups)
        if (!isGroup && isOnline) {
            Circle presenceIndicator = new Circle(5);
            presenceIndicator.getStyleClass().addAll("presence-indicator", "presence-online");
            StackPane.setAlignment(presenceIndicator, Pos.BOTTOM_RIGHT);
            StackPane.setMargin(presenceIndicator, new Insets(0, 2, 2, 0));
            stack.getChildren().add(presenceIndicator);
        }

        return stack;
    }

    public String getName() {
        return name;
    }

    public void setPreview(String text) {
        previewLabel.setText(text);
    }

    public void setUnreadCount(int count) {
        unreadLabel.setText(String.valueOf(count));
        unreadLabel.setVisible(count > 0);
        unreadLabel.setManaged(count > 0);
    }

    public void setTime(String time) {
        timeLabel.setText(time);
    }
}
