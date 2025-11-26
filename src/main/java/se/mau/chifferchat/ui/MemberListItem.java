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
 * Custom cell component for member list items in the right drawer.
 * Displays member avatar, name, status, and presence indicator.
 */
public class MemberListItem extends HBox {

    private final String username;
    private final boolean isOnline;
    private final Label nameLabel;
    private final Label statusLabel;

    public MemberListItem(String username, boolean isOnline, String status) {
        this.username = username;
        this.isOnline = isOnline;

        getStyleClass().add("member-item");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(10);
        setPadding(new Insets(6, 8, 6, 8));

        // Avatar with presence indicator
        StackPane avatarContainer = createAvatar();

        // Member info
        VBox infoBox = new VBox(2);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        nameLabel = new Label(username);
        nameLabel.getStyleClass().add("member-name");

        statusLabel = new Label(status != null ? status : (isOnline ? "Online" : "Offline"));
        statusLabel.getStyleClass().add("member-status");

        infoBox.getChildren().addAll(nameLabel, statusLabel);

        getChildren().addAll(avatarContainer, infoBox);
    }

    private StackPane createAvatar() {
        StackPane stack = new StackPane();
        stack.getStyleClass().add("member-avatar");
        stack.setPrefSize(32, 32);
        stack.setMinSize(32, 32);
        stack.setMaxSize(32, 32);

        // Avatar circle
        Circle circle = new Circle(16);
        circle.getStyleClass().add("chat-avatar-circle");

        // Avatar text (first letter of username)
        String avatarText = username.substring(0, Math.min(1, username.length())).toUpperCase();
        Label avatarLabel = new Label(avatarText);
        avatarLabel.getStyleClass().add("chat-avatar-text");
        avatarLabel.setStyle("-fx-font-size: 14px;");

        stack.getChildren().addAll(circle, avatarLabel);

        // Presence indicator
        Circle presenceIndicator = new Circle(4);
        presenceIndicator.getStyleClass().addAll("presence-indicator",
                isOnline ? "presence-online" : "presence-offline");
        StackPane.setAlignment(presenceIndicator, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(presenceIndicator, new Insets(0, 0, 0, 0));
        stack.getChildren().add(presenceIndicator);

        return stack;
    }

    public String getUsername() {
        return username;
    }

    public void setStatus(String status) {
        statusLabel.setText(status);
    }
}

