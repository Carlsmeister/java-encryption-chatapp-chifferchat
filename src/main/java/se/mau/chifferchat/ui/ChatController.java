package se.mau.chifferchat.ui;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.util.Duration;
import se.mau.chifferchat.client.Client;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Timer;
import java.util.TimerTask;

public class ChatController {

    private final DateTimeFormatter clockFormat = DateTimeFormatter.ofPattern("HH:mm");
    private final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm");
    public ImageView userAvatar;
    public Label usernameLabel;
    private Client client;
    @FXML
    private Label clockLabel;
    @FXML
    private Label connectionStatus;
    @FXML
    private ListView<ChatMessage> chatList;
    @FXML
    private ListView<String> userList;
    @FXML
    private TextField messageField;
    @FXML
    private Button sendButton;
    @FXML
    private Button settingsButton;
    @FXML
    private Button logoutButton;

    @FXML
    public void initialize() {
        this.client = HelloApplication.getClient();
        client.setController(this);

        String me = client.getUsername();
        if (me != null && !me.isBlank()) {
            usernameLabel.setText(me + " (You)");
        }

        Platform.runLater(() -> messageField.requestFocus());

        startClock();

        userList.getItems().addAll("Carl", "Alex", "Maya", "Evelyn");
        styleUserList();

        chatList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(ChatMessage item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Label msg = new Label(item.text());
                msg.getStyleClass().add("chat-message");
                Label ts = new Label(item.time().format(timeFormat));
                ts.getStyleClass().add("text-muted");
                ts.setMinWidth(40);

                Region leftSpacer = new Region();
                Region rightSpacer = new Region();
                HBox.setHgrow(leftSpacer, Priority.ALWAYS);
                HBox.setHgrow(rightSpacer, Priority.ALWAYS);
                HBox hbox;
                switch (item.type()) {
                    case SENT -> {
                        msg.getStyleClass().add("sent");
                        hbox = new HBox(leftSpacer, ts, msg);
                        hbox.setSpacing(8);
                    }
                    case RECEIVED -> {
                        msg.getStyleClass().add("received");
                        hbox = new HBox(msg, ts, rightSpacer);
                        hbox.setSpacing(8);
                    }
                    default -> { // SYSTEM
                        msg.getStyleClass().add("system-message");
                        hbox = new HBox(leftSpacer, msg, rightSpacer);
                        hbox.setSpacing(8);
                    }
                }
                setGraphic(hbox);
                setText(null);
            }
        });

        sendButton.setOnAction(e -> sendMessage());
        messageField.setOnAction(e -> sendMessage());

        setConnectionStatus(client.getOut() != null);

        Stage stage = SceneManager.getStage();

        stage.setOnCloseRequest(event -> {
            event.consume();
            client.disconnect();
            Platform.exit();
            System.exit(0);
        });
    }

    private void styleUserList() {
        userList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                setText("\uD83D\uDC64 " + item);
            }
        });
    }

    private void startClock() {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> clockLabel.setText(LocalTime.now().format(clockFormat)));
            }
        }, 0, 1000);
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        if (message.isEmpty()) return;

        if (client != null) {
            client.sendMessage(message);
        }

        String label = (client.getUsername() != null ? "You" : "Me") + ": " + message;
        chatList.getItems().add(new ChatMessage(label, LocalDateTime.now(), MessageType.SENT));
        autoScroll();
        messageField.clear();
    }

    public void receiveMessage(String message) {

        MessageType type = MessageType.RECEIVED;
        if (message.endsWith(" joined the chat!") || message.endsWith(" left the chat!")) {
            type = MessageType.SYSTEM;
            updateUserList(message);
        } else if (!message.contains(": ")) {
            type = MessageType.SYSTEM;
        }
        chatList.getItems().add(new ChatMessage(message, LocalDateTime.now(), type));
        autoScroll();
        flashNewMessage();
    }

    private void flashNewMessage() {
        String prev = chatList.getStyle();
        chatList.setStyle("-fx-background-color: #3a3d43;");
        PauseTransition pt = new PauseTransition(Duration.millis(120));
        pt.setOnFinished(e -> chatList.setStyle(prev));
        pt.play();
    }

    private void updateUserList(String message) {
        String name = message.replace(" joined the chat!", "").replace(" left the chat!", "");
        if (message.contains(" joined the chat!")) {
            if (!userList.getItems().contains(name)) userList.getItems().add(name);
        } else if (message.contains(" left the chat!")) {
            userList.getItems().remove(name);
        }
    }

    private void appendSystemMessage(String text) {
        chatList.getItems().add(new ChatMessage(text, LocalDateTime.now(), MessageType.SYSTEM));
        autoScroll();
    }

    private void autoScroll() {
        Platform.runLater(() -> chatList.scrollTo(chatList.getItems().size() - 1));
    }

    public void setConnectionStatus(boolean online) {
        if (connectionStatus == null) return;
        connectionStatus.setText(online ? "🟢" : "🔴");
        connectionStatus.getStyleClass().removeAll("status-online", "status-offline");
        connectionStatus.getStyleClass().add(online ? "status-online" : "status-offline");
    }

    @FXML
    private void onSettings() {
        appendSystemMessage("Settings are not implemented yet.");
    }

    @FXML
    private void onLogout() {
        if (client != null) client.sendMessage("/quit");
        appendSystemMessage("You left the chat.");
        logoutToLogin();
    }

    private void logoutToLogin() {
        try {
            HelloApplication.resetClient();
            SceneManager.switchScene("/se/mau/chifferchat/login-view.fxml", "ChifferChat – Login");
        } catch (Exception ignored) {
        }
    }

    private enum MessageType {SENT, RECEIVED, SYSTEM}

    private record ChatMessage(String text, LocalDateTime time, MessageType type) {
    }
}
