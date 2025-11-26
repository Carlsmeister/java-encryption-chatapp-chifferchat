package se.mau.chifferchat.ui;

import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import se.mau.chifferchat.client.Client;
import se.mau.chifferchat.common.Group;
import se.mau.chifferchat.crypto.CryptoKeyGenerator;
import se.mau.chifferchat.crypto.Encryption;

import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Modern ChatController with Discord-like interface.
 * Implements dark/light theme switching, member drawer, and enhanced UI.
 */
public class ChatController implements IChatController {

    private final DateTimeFormatter clockFormat = DateTimeFormatter.ofPattern("HH:mm");
    private Client client;
    private static final PseudoClass ACTIVE_PSEUDO_CLASS = PseudoClass.getPseudoClass("active");
    // Message history storage: key = username or groupId, value = list of message
    // records
    private final Map<String, List<MessageRecord>> messageHistory = new HashMap<>();
    private boolean isDarkTheme = true;
    private String currentChatUser = null;
    private Group currentChatGroup = null;
    private boolean isMemberDrawerOpen = false;
    private List<String> onlineUsers = new ArrayList<>();
    private LocalDateTime lastMessageDate = null;
    // FXML Navigation Bar
    @FXML
    private Button homeButton;
    @FXML
    private Button friendsButton;
    @FXML
    private Button groupsButton;
    @FXML
    private Button settingsButton;
    @FXML
    private Button themeToggleButton;
    @FXML
    private Label themeToggleIcon;
    @FXML
    private Label userAvatarLabel;

    // FXML Chat List Panel
    @FXML
    private TextField searchField;
    @FXML
    private Button tabAllChats;
    @FXML
    private Button tabGroups;
    @FXML
    private Button tabContacts;
    @FXML
    private ListView<Node> chatListView;
    @FXML
    private Button createGroupButton;

    // FXML Chat Window
    @FXML
    private Label chatAvatarLabel;
    @FXML
    private Label chatTitleLabel;
    @FXML
    private Label chatSubtitleLabel;
    @FXML
    private Label encryptionStatusLabel;
    @FXML
    private Label clockLabel;
    @FXML
    private Button memberCountButton;
    @FXML
    private Button callButton;
    @FXML
    private ScrollPane messagesScrollPane;
    @FXML
    private VBox messagesVBox;
    @FXML
    private TextField messageField;
    @FXML
    private Button sendButton;

    // FXML Member Drawer
    @FXML
    private VBox memberDrawer;
    @FXML
    private Button closeMemberDrawerButton;
    @FXML
    private Button addMemberButton;
    @FXML
    private ListView<Node> memberListView;
    private ChatTab currentTab = ChatTab.ALL_CHATS;

    @FXML
    public void initialize() {
        this.client = HelloApplication.getClient();
        client.setController(this);

        String username = client.getUsername();
        if (username != null && !username.isEmpty()) {
            userAvatarLabel.setText(username.substring(0, 1).toUpperCase());
        }

        // Setup clock
        startClock();

        // Setup search field
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterChatList(newVal));

        // Setup active tab
        updateTabStyles();

        // Load initial chat list
        loadChatList();

        // Setup message field
        messageField.setOnAction(e -> sendMessage());
        Platform.runLater(() -> messageField.requestFocus());

        // Disable send controls until a chat is selected
        updateSendControls();

        // Setup window close handler
        Platform.runLater(() -> {
            Stage stage = SceneManager.getStage();
            if (stage != null) {
                stage.setOnCloseRequest(event -> {
                    event.consume();
                    client.disconnect();
                    Platform.exit();
                    System.exit(0);
                });
            }
        });

        // Show welcome message
        appendSystemMessage("Welcome to ChifferChat! Select a user to start chatting.");
    }

    @FXML
    private void onHomeClick() {
        currentTab = ChatTab.ALL_CHATS;
        updateTabStyles();
        loadChatList();
        setNavButtonActive(homeButton);
    }

    @FXML
    private void onFriendsClick() {
        currentTab = ChatTab.CONTACTS;
        updateTabStyles();
        loadChatList();
        setNavButtonActive(friendsButton);
    }

    // ============ NAVIGATION ACTIONS ============

    @FXML
    private void onGroupsClick() {
        currentTab = ChatTab.GROUPS;
        updateTabStyles();
        loadChatList();
        client.requestGroups();
        setNavButtonActive(groupsButton);
    }

    @FXML
    private void onSettings() {
        appendSystemMessage("Settings panel coming soon!");
    }

    @FXML
    private void onThemeToggle() {
        isDarkTheme = !isDarkTheme;
        applyTheme();
    }

    private void applyTheme() {
        Platform.runLater(() -> {
            Stage stage = SceneManager.getStage();
            if (stage != null && stage.getScene() != null && stage.getScene().getRoot() != null) {
                if (isDarkTheme) {
                    stage.getScene().getRoot().getStyleClass().remove("light-theme");
                    themeToggleIcon.setText("🌙");
                } else {
                    if (!stage.getScene().getRoot().getStyleClass().contains("light-theme")) {
                        stage.getScene().getRoot().getStyleClass().add("light-theme");
                    }
                    themeToggleIcon.setText("☀️");
                }
            }
        });
    }

    private void setNavButtonActive(Button activeButton) {
        homeButton.pseudoClassStateChanged(ACTIVE_PSEUDO_CLASS, activeButton == homeButton);
        friendsButton.pseudoClassStateChanged(ACTIVE_PSEUDO_CLASS, activeButton == friendsButton);
        groupsButton.pseudoClassStateChanged(ACTIVE_PSEUDO_CLASS, activeButton == groupsButton);
    }

    @FXML
    private void onTabAllChats() {
        currentTab = ChatTab.ALL_CHATS;
        updateTabStyles();
        loadChatList();
    }

    @FXML
    private void onTabGroups() {
        currentTab = ChatTab.GROUPS;
        updateTabStyles();
        loadChatList();
        client.requestGroups();
    }

    // ============ TAB ACTIONS ============

    @FXML
    private void onTabContacts() {
        currentTab = ChatTab.CONTACTS;
        updateTabStyles();
        loadChatList();
    }

    private void updateTabStyles() {
        tabAllChats.getStyleClass().remove("chat-tab-active");
        tabGroups.getStyleClass().remove("chat-tab-active");
        tabContacts.getStyleClass().remove("chat-tab-active");

        switch (currentTab) {
            case ALL_CHATS -> tabAllChats.getStyleClass().add("chat-tab-active");
            case GROUPS -> tabGroups.getStyleClass().add("chat-tab-active");
            case CONTACTS -> tabContacts.getStyleClass().add("chat-tab-active");
        }
    }

    private void loadChatList() {
        chatListView.getItems().clear();

        List<String> usersOnline = new ArrayList<>(onlineUsers);

        switch (currentTab) {
            case ALL_CHATS, CONTACTS -> {
                for (String user : usersOnline) {
                    if (!user.equals(client.getUsername())) {
                        boolean isOnline = onlineUsers.contains(user);
                        ChatListItem item = new ChatListItem(
                                user,
                                "Click to start chatting...",
                                isOnline,
                                false,
                                0,
                                "12:00");
                        item.setOnMouseClicked(e -> selectChat(user, false));
                        chatListView.getItems().add(item);
                    }
                }
            }
            case GROUPS -> {
                // Load actual groups from client
                Collection<Group> groupsCollection = client.getAllGroups();
                if (groupsCollection != null) {
                    for (Group group : groupsCollection) {
                        ChatListItem item = new ChatListItem(
                                group.getGroupName(),
                                group.getMemberCount() + " members",
                                false,
                                true,
                                0,
                                "Group");
                        item.setOnMouseClicked(e -> selectGroup(group));
                        chatListView.getItems().add(item);
                    }
                }
            }
        }

        // Reapply current search filter to keep UX consistent
        if (searchField != null && searchField.getText() != null && !searchField.getText().isBlank()) {
            filterChatList(searchField.getText());
        }
    }

    private void filterChatList(String query) {
        if (query == null || query.trim().isEmpty()) {
            loadChatList();
            return;
        }

        String lowerQuery = query.toLowerCase();
        chatListView.getItems().removeIf(node -> {
            if (node instanceof ChatListItem item) {
                return !item.getName().toLowerCase().contains(lowerQuery);
            }
            return false;
        });
    }

    // ============ CHAT LIST ============

    private void selectChat(String username, boolean isGroup) {
        if (isGroup) {
            // Handle group selection - find the actual group object
            currentChatUser = null;
            Collection<Group> groupsCollection = client.getAllGroups();
            if (groupsCollection != null) {
                for (Group g : groupsCollection) {
                    if (g.getGroupName().equals(username)) {
                        currentChatGroup = g;
                        selectGroup(g);
                        return;
                    }
                }
            }
        } else {
            currentChatUser = username;
            currentChatGroup = null;
        }

        // Check if user is online
        boolean isOnline = onlineUsers.contains(username);
        String statusText = isOnline ? "Online" : "Offline";

        // Update chat header
        chatTitleLabel.setText(username);
        chatSubtitleLabel.setText(isGroup ? "Group • X members" : statusText);
        chatAvatarLabel.setText(username.substring(0, 1).toUpperCase());

        // Update encryption status
        boolean isEncrypted = !isGroup || (currentChatGroup != null);
        encryptionStatusLabel.setText(isEncrypted ? "🔒" : "🔓");

        // Show/hide member count button
        memberCountButton.setVisible(isGroup);
        memberCountButton.setManaged(isGroup);

        // Clear messages display
        messagesVBox.getChildren().clear();
        lastMessageDate = null;

        // Load message history for this user
        loadMessageHistory(username);

        // Close member drawer
        if (isMemberDrawerOpen) {
            toggleMemberDrawer();
        }

        // Request public key
        if (!isGroup) {
            PublicKey key = client.getPublicKeyForUser(username);
            if (key == null) {
                client.sendMessage("/getkey " + username);
            }
        }

        updateSendControls();
    }

    @FXML
    private void sendMessage() {
        String message = messageField.getText().trim();
        if (message.isEmpty())
            return;

        if (currentChatGroup != null) {
            sendGroupMessage(message);
        } else if (currentChatUser != null) {
            sendPrivateMessage(message);
        } else {
            appendSystemMessage("Please select a chat first.");
        }

        messageField.clear();
    }

    private void sendPrivateMessage(String message) {
        PublicKey receiverPublicKey = client.getPublicKeyForUser(currentChatUser);

        if (receiverPublicKey == null) {
            client.sendMessage("/getkey " + currentChatUser);

            waitForPublicKey(currentChatUser, 1000)
                    .thenAcceptAsync(pubKey -> {
                        if (pubKey != null) {
                            encryptAndSendMessage(message, currentChatUser, pubKey);
                        } else {
                            appendSystemMessage("Cannot send: No public key for " + currentChatUser);
                        }
                    }, Platform::runLater)
                    .exceptionally(ex -> {
                        appendSystemMessage("Error fetching key: " + ex.getMessage());
                        return null;
                    });
        } else {
            encryptAndSendMessage(message, currentChatUser, receiverPublicKey);
        }

        // Save and show sent message
        // Save to history with recipient's username as key
        messageHistory.computeIfAbsent(currentChatUser, k -> new ArrayList<>())
                .add(new MessageRecord(message, LocalDateTime.now(), MessageBubble.MessageType.SENT, null));

        // Display the message (don't save again since we just did)
        addMessage(message, LocalDateTime.now(), MessageBubble.MessageType.SENT, null, false);
    }

    // ============ MESSAGE SENDING ============

    private void sendGroupMessage(String message) {
        if (currentChatGroup == null)
            return;

        try {
            List<String> members = currentChatGroup.getMembers();
            SecretKey aesKey = CryptoKeyGenerator.generateAESKey();
            GCMParameterSpec iv = CryptoKeyGenerator.generateIv();

            String encryptedMessage = Encryption.encryptAES(message, aesKey, iv);
            String ivBase64 = Base64.getEncoder().encodeToString(iv.getIV());

            StringBuilder keys = new StringBuilder();
            List<String> missingKeys = new ArrayList<>();
            for (String member : members) {
                if (member.equals(client.getUsername()))
                    continue;

                PublicKey memberKey = client.getPublicKeyForUser(member);
                if (memberKey != null) {
                    if (!keys.isEmpty())
                        keys.append("|");
                    String encKey = Encryption.encryptAESKeyRSA(aesKey, memberKey);
                    keys.append(member).append(":").append(encKey);
                } else {
                    // request key and mark as missing for this send
                    client.sendMessage("/getkey " + member);
                    missingKeys.add(member);
                }
            }

            if (keys.isEmpty()) {
                appendSystemMessage("Cannot send to group yet: waiting for members' public keys ("
                        + String.join(", ", missingKeys) + ")");
                return;
            }

            String fullMessage = keys + "|" + ivBase64 + ":" + encryptedMessage;
            client.sendGroupMessage(currentChatGroup.getGroupId(), fullMessage);

            // Save and show sent message
            // Save to history with group ID as key
            messageHistory.computeIfAbsent(currentChatGroup.getGroupId(), k -> new ArrayList<>())
                    .add(new MessageRecord(message, LocalDateTime.now(), MessageBubble.MessageType.SENT,
                            client.getUsername()));

            // Display the message (don't save again since we just did)
            addMessage(message, LocalDateTime.now(), MessageBubble.MessageType.SENT, client.getUsername(), false);

            if (!missingKeys.isEmpty()) {
                appendSystemMessage(
                        "Delivered to available members. Still awaiting keys for: " + String.join(", ", missingKeys));
            }

        } catch (Exception e) {
            appendSystemMessage("Encryption failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void receiveMessage(String message) {
        MessageBubble.MessageType type = MessageBubble.MessageType.RECEIVED;
        String sender = null;
        String chatKey = null;
        boolean shouldDisplay = true;

        if (message.startsWith("Welcome ")) {
            return;
        }

        // Check if system message (join/leave notifications)
        if (message.endsWith(" joined the chat!") || message.endsWith(" left the chat!")) {
            type = MessageBubble.MessageType.SYSTEM;
            updateUserList(message);
        } else if (!message.contains(": ")) {
            type = MessageBubble.MessageType.SYSTEM;
        } else {
            // Extract sender and message
            int colonIndex = message.indexOf(": ");
            if (colonIndex > 0) {
                sender = message.substring(0, colonIndex);
                String messageText = message.substring(colonIndex + 2);

                // Determine the chat key (which conversation this belongs to)
                if (currentChatGroup != null) {
                    // We're in a group chat, this message belongs to the group
                    chatKey = currentChatGroup.getGroupId();
                } else {
                    // This is a private message - belongs to sender's chat
                    chatKey = sender;
                }

                // ALWAYS save the message to history
                if (type != MessageBubble.MessageType.SYSTEM && chatKey != null) {
                    messageHistory.computeIfAbsent(chatKey, k -> new ArrayList<>())
                            .add(new MessageRecord(messageText, LocalDateTime.now(), type, sender));
                }

                // Only DISPLAY if we're in the correct chat
                if (currentChatGroup == null && currentChatUser != null && sender != null) {
                    if (!sender.equals(currentChatUser)) {
                        shouldDisplay = false; // Don't display, but we already saved it above
                    }
                }

                // Update message to just the text for display
                message = messageText;
            }
        }

        // Only display the message if appropriate
        if (shouldDisplay) {
            addMessage(message, LocalDateTime.now(), type, sender, false); // false = don't save again
            flashNewMessage();
        }
    }

    private void addMessage(String text, LocalDateTime time, MessageBubble.MessageType type, String sender) {
        addMessage(text, time, type, sender, true);
    }

    private CompletableFuture<PublicKey> waitForPublicKey(String username, long maxWaitMs) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            int pollInterval = 50;

            while (System.currentTimeMillis() - startTime < maxWaitMs) {
                PublicKey key = client.getPublicKeyForUser(username);
                if (key != null) {
                    return key;
                }

                try {
                    Thread.sleep(pollInterval);
                    pollInterval = Math.min(pollInterval + 25, 200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
            return null;
        });
    }

    private void encryptAndSendMessage(String message, String targetUser, PublicKey receiverPublicKey) {
        try {
            SecretKey aesKey = CryptoKeyGenerator.generateAESKey();
            GCMParameterSpec iv = CryptoKeyGenerator.generateIv();

            String encryptedMessage = Encryption.encryptAES(message, aesKey, iv);
            String encryptedAESKey = Encryption.encryptAESKeyRSA(aesKey, receiverPublicKey);
            String ivBase64 = Base64.getEncoder().encodeToString(iv.getIV());

            String fullMessage = encryptedAESKey + ":" + ivBase64 + ":" + encryptedMessage;
            client.sendMessage(fullMessage);

        } catch (Exception e) {
            System.err.println("Encryption failed: " + e.getMessage());
            Platform.runLater(() -> appendSystemMessage("Failed to encrypt message"));
        }
    }

    // ============ MESSAGE RECEIVING ============

    private void addMessage(String text, LocalDateTime time, MessageBubble.MessageType type, String sender,
                            boolean saveToHistory) {
        // Save to history if not a system message and we have an active chat
        if (saveToHistory && type != MessageBubble.MessageType.SYSTEM) {
            String chatKey = currentChatGroup != null ? currentChatGroup.getGroupId() : currentChatUser;
            if (chatKey != null) {
                messageHistory.computeIfAbsent(chatKey, k -> new ArrayList<>())
                        .add(new MessageRecord(text, time, type, sender));
            }
        }

        Platform.runLater(() -> {
            // Add timestamp divider if date changed
            if (lastMessageDate == null || !lastMessageDate.toLocalDate().equals(time.toLocalDate())) {
                HBox divider = MessageBubble.createTimestampDivider(
                        MessageBubble.formatTimestampDivider(time));
                messagesVBox.getChildren().add(divider);
                lastMessageDate = time;
            }

            // Create message bubble
            boolean isGroupChat = currentChatGroup != null;
            HBox messageBubble = MessageBubble.createMessageBubble(text, time, type, sender, isGroupChat);
            messagesVBox.getChildren().add(messageBubble);

            // Auto-scroll
            autoScroll();
        });
    }

    private void appendSystemMessage(String text) {
        addMessage(text, LocalDateTime.now(), MessageBubble.MessageType.SYSTEM, null);
    }

    private void loadMessageHistory(String chatKey) {
        List<MessageRecord> history = messageHistory.get(chatKey);
        if (history != null && !history.isEmpty()) {
            for (MessageRecord record : history) {
                // Use saveToHistory=false to avoid re-saving when loading
                addMessage(record.text, record.time, record.type, record.sender, false);
            }
        } else {
            // Show welcome message for new chats
            if (currentChatGroup != null) {
                appendSystemMessage(
                        "Group chat: " + currentChatGroup.getGroupName() + ". All messages are encrypted. 🔒");
            } else if (currentChatUser != null) {
                appendSystemMessage("Chat with " + currentChatUser + " started. All messages are encrypted. 🔒");
            }
        }
    }

    private void updateUserList(String message) {
        String name = message.replace(" joined the chat!", "").replace(" left the chat!", "");
        // Request updated online users list from server
        if (message.contains(" joined") || message.contains(" left")) {
            client.requestOnlineUsers();
        }
    }

    private void flashNewMessage() {
        if (messagesScrollPane != null) {
            String prev = messagesScrollPane.getStyle();
            messagesScrollPane.setStyle(prev + "; -fx-background-color: derive(-fx-color-bg-tertiary, 5%);");
            PauseTransition pt = new PauseTransition(Duration.millis(120));
            pt.setOnFinished(e -> messagesScrollPane.setStyle(prev));
            pt.play();
        }
    }

    private void autoScroll() {
        Platform.runLater(() -> {
            if (messagesScrollPane != null) {
                messagesScrollPane.setVvalue(1.0);
            }
        });
    }

    @FXML
    private void onToggleMemberDrawer() {
        toggleMemberDrawer();
    }

    private void toggleMemberDrawer() {
        isMemberDrawerOpen = !isMemberDrawerOpen;

        if (isMemberDrawerOpen) {
            // Open drawer
            memberDrawer.setVisible(true);
            memberDrawer.setManaged(true);

            TranslateTransition transition = new TranslateTransition(Duration.millis(250), memberDrawer);
            transition.setFromX(240);
            transition.setToX(0);

            memberDrawer.setPrefWidth(240);
            memberDrawer.setMinWidth(240);
            memberDrawer.setMaxWidth(240);

            transition.play();

            // Load members
            loadMemberList();
        } else {
            // Close drawer
            TranslateTransition transition = new TranslateTransition(Duration.millis(250), memberDrawer);
            transition.setFromX(0);
            transition.setToX(240);
            transition.setOnFinished(e -> {
                memberDrawer.setVisible(false);
                memberDrawer.setManaged(false);
                memberDrawer.setPrefWidth(0);
                memberDrawer.setMinWidth(0);
                memberDrawer.setMaxWidth(0);
            });
            transition.play();
        }
    }

    // ============ MEMBER DRAWER ============

    private void loadMemberList() {
        memberListView.getItems().clear();

        if (currentChatGroup != null) {
            List<String> members = currentChatGroup.getMembers();
            for (String member : members) {
                boolean isOnline = onlineUsers.contains(member);
                String status = isOnline ? "Online" : "Offline";
                MemberListItem item = new MemberListItem(member, isOnline, status);
                memberListView.getItems().add(item);
            }
        }
    }

    @FXML
    private void onCreateGroup() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Create Group");
        dialog.setHeaderText("Create a new encrypted group");
        dialog.setContentText("Group name:");

        dialog.showAndWait().ifPresent(groupName -> {
            if (!groupName.trim().isEmpty()) {
                client.createGroup(groupName.trim());
                appendSystemMessage("Creating group: " + groupName);
            }
        });
    }

    @FXML
    private void onAddMember() {
        if (currentChatGroup == null) {
            appendSystemMessage("Please select a group first");
            return;
        }

        client.requestOnlineUsers();

        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).thenRun(() -> Platform.runLater(() -> {
            if (onlineUsers.isEmpty()) {
                appendSystemMessage("No online users found");
                return;
            }

            ChoiceDialog<String> dialog = new ChoiceDialog<>(null, onlineUsers);
            dialog.setTitle("Add Member");
            dialog.setHeaderText("Add member to " + currentChatGroup.getGroupName());
            dialog.setContentText("Select user:");

            dialog.showAndWait().ifPresent(username -> {
                client.addToGroup(currentChatGroup.getGroupId(), username);
                appendSystemMessage("Added " + username + " to group");
                loadMemberList();
            });
        }));
    }

    // ============ GROUP ACTIONS ============

    @FXML
    private void onAddToGroup() {
        appendSystemMessage("Add to group feature coming soon!");
    }

    public void onGroupCreated(Group group) {
        appendSystemMessage("Group created: " + group.getGroupName());
        client.requestGroups();
        if (currentTab == ChatTab.GROUPS) {
            loadChatList();
        }
    }

    public void refreshGroups(List<Group> groups) {
        // Just reload the chat list if we're on the groups tab
        // loadChatList() already fetches groups from the client
        Platform.runLater(() -> {
            if (currentTab == ChatTab.GROUPS) {
                loadChatList();
            }
        });
    }

    public void selectGroup(Group group) {
        currentChatGroup = group;
        currentChatUser = null;

        chatTitleLabel.setText(group.getGroupName());
        chatSubtitleLabel.setText("Group • " + group.getMemberCount() + " members");
        chatAvatarLabel.setText("🔒");
        encryptionStatusLabel.setText("🔒");

        memberCountButton.setVisible(true);
        memberCountButton.setManaged(true);
        memberCountButton.setText("👥 " + group.getMemberCount());

        // Clear messages display
        messagesVBox.getChildren().clear();
        lastMessageDate = null;

        // Load message history for this group
        loadMessageHistory(group.getGroupId());

        // Request group members and keys
        client.requestGroupMembers(group.getGroupId());
        for (String member : group.getMembers()) {
            if (client.getPublicKeyForUser(member) == null) {
                client.sendMessage("/getkey " + member);
            }
        }

        updateSendControls();
    }

    public void updateOnlineUsers(List<String> users) {
        System.out.println("updateOnlineUsers called with: " + users);
        this.onlineUsers = new ArrayList<>(users);
        // Refresh chat list to reflect online status changes
        Platform.runLater(this::loadChatList);
    }

    public Group getCurrentGroup() {
        return currentChatGroup;
    }

    public void appendGroupMessage(String message) {
        receiveMessage(message);
    }

    @FXML
    private void onCall() {
        appendSystemMessage("Voice/video call feature coming soon!");
    }

    @FXML
    private void onClearHistory() {
        messagesVBox.getChildren().clear();
        lastMessageDate = null;
        appendSystemMessage("Chat history cleared.");
    }

    // ============ OTHER ACTIONS ============

    @FXML
    private void onMute() {
        appendSystemMessage("Mute notifications feature coming soon!");
    }

    @FXML
    private void onLogout() {
        if (client != null) {
            client.sendMessage("/quit");
        }
        appendSystemMessage("Logging out...");
        logoutToLogin();
    }

    private void logoutToLogin() {
        try {
            HelloApplication.resetClient();
            SceneManager.switchScene("/se/mau/chifferchat/login-view.fxml", "ChifferChat – Login");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onEmojiPicker() {
        // Emoji picker not implemented
    }

    @FXML
    private void onAttachment() {
        appendSystemMessage("File attachment feature coming soon!");
    }

    @FXML
    private void onVoiceNote() {
        appendSystemMessage("Voice note feature coming soon!");
    }

    private void startClock() {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    if (clockLabel != null) {
                        clockLabel.setText(LocalTime.now().format(clockFormat));
                    }
                });
            }
        }, 0, 1000);
    }

    public void setConnectionStatus(boolean online) {
        // Connection status can be shown in subtitle or as indicator
        Platform.runLater(() -> {
            if (currentChatUser != null && !currentChatUser.isEmpty()) {
                chatSubtitleLabel.setText(online ? "Online" : "Offline");
            }
        });
    }

    // ============ UTILITIES ============

    @Override
    public void onGroupMembersUpdated(Group group) {
        // Update header counts if this is the same group currently open
        if (currentChatGroup != null && currentChatGroup.getGroupId().equals(group.getGroupId())) {
            // Keep currentChatGroup reference, but update header and drawer
            chatSubtitleLabel.setText("Group • " + group.getMemberCount() + " members");
            memberCountButton.setText("👥 " + group.getMemberCount());
            // If member drawer open, reload
            if (isMemberDrawerOpen) {
                loadMemberList();
            }
        }

        // Also refresh chat list if we are in groups tab
        if (currentTab == ChatTab.GROUPS) {
            loadChatList();
        }
    }

    private void updateSendControls() {
        boolean enabled = (currentChatUser != null) || (currentChatGroup != null);
        sendButton.setDisable(!enabled);
        messageField.setDisable(!enabled);
    }

    // ============ GROUP UPDATE HANDLERS ============

    private enum ChatTab {
        ALL_CHATS,
        GROUPS,
        CONTACTS
    }

    // ============ INPUT ENABLE/DISABLE ============

    // Inner class to store message data
    private static class MessageRecord {
        String text;
        LocalDateTime time;
        MessageBubble.MessageType type;
        String sender;

        MessageRecord(String text, LocalDateTime time, MessageBubble.MessageType type, String sender) {
            this.text = text;
            this.time = time;
            this.type = type;
            this.sender = sender;
        }
    }
}
