package se.mau.chifferchat.client;

import javafx.application.Platform;
import se.mau.chifferchat.common.Group;
import se.mau.chifferchat.crypto.CryptoKeyGenerator;
import se.mau.chifferchat.crypto.Decryption;
import se.mau.chifferchat.ui.IChatController;

import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

public class Client implements Runnable {
    private Socket client;
    private BufferedReader in;
    private PrintWriter out;
    private volatile boolean listening = true;
    private volatile boolean loggedIn = false;

    private final Map<String, Group> groups = new HashMap<>();
    private volatile String username;

    private final Map<String, PublicKey> publicKeyCache = new HashMap<>();
    private volatile IChatController controller;
    private PublicKey publicKey;
    private PrivateKey privateKey;

    public Client() {
    }

    public void connect() {
        Thread clientThread = new Thread(this);
        clientThread.setDaemon(true);
        clientThread.setName("Client Thread");
        clientThread.start();
    }

    @Override
    public void run() {
        try {
            client = new Socket("localhost", 5090);
            out = new PrintWriter(client.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));

            try {
                KeyPair keyPair = CryptoKeyGenerator.generateRSAKeyPair();
                this.publicKey = keyPair.getPublic();
                this.privateKey = keyPair.getPrivate();
            } catch (NoSuchAlgorithmException e) {
                System.err.println("Failed to generate RSA keypair: " + e.getMessage());
                shutdown();
                return;
            }

            if (controller != null) {
                Platform.runLater(() -> controller.setConnectionStatus(true));
            }

            int waitCount = 0;
            while (!loggedIn && waitCount < 100) { // 5 second timeout
                Thread.sleep(50);
                waitCount++;
            }

            if (!loggedIn) {
                System.err.println("Login timeout - no username provided");
                shutdown();
                return;
            }

            if (username != null && out != null) {
                out.println(username);
                System.out.println("Sent username: " + username);
            } else {
                System.err.println("Username is null, cannot proceed");
                shutdown();
                return;
            }

            String publicKeyB64 = Base64.getEncoder().encodeToString(publicKey.getEncoded());
            out.println("/pubkey " + publicKeyB64);
            System.out.println("Sent public key");

            String line;

            while (listening && (line = in.readLine()) != null) {

                if (line.startsWith("/key ")) {
                    try {
                        String[] parts = line.split(" ", 3);
                        String targetUser = parts[1];
                        byte[] keyBytes = Base64.getDecoder().decode(parts[2]);
                        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
                        KeyFactory kf = KeyFactory.getInstance("RSA");
                        PublicKey targetPubKey = kf.generatePublic(spec);
                        publicKeyCache.put(targetUser, targetPubKey);
                        System.out.println("Stored public key for user: " + targetUser);
                    } catch (Exception e) {
                        System.err.println("Failed to parse public key: " + e.getMessage());
                    }
                    continue;
                }

                // Group commands
                if (line.startsWith("/groupcreated ")) {
                    String[] parts = line.substring(14).split(" ", 2);
                    String groupId = parts[0];
                    String groupName = parts[1];
                    Group group = new Group(groupId, groupName, username, List.of(username),
                            System.currentTimeMillis());
                    groups.put(groupId, group);
                    if (controller != null) {
                        Platform.runLater(() -> controller.onGroupCreated(group));
                    }
                    continue;
                }
                // Group message with explicit group id prefix
                if (line.startsWith("/g ")) {
                    // Format: /g <groupId> [Sender]: <encryptedPart>
                    try {
                        int firstSpace = line.indexOf(' ', 3);
                        if (firstSpace > 0) {
                            String groupId = line.substring(3, firstSpace);
                            String rest = line.substring(firstSpace + 1);

                            // Only process if user is currently viewing this group
                            if (controller != null && controller.getCurrentGroup() != null
                                    && groupId.equals(controller.getCurrentGroup().getGroupId())) {

                                // Expect rest like: [Sender]: <payload>
                                int colonIdx = rest.indexOf(": ");
                                if (colonIdx > 0) {
                                    String senderName = rest.substring(1, rest.indexOf(']'));
                                    String encryptedPart = rest.substring(colonIdx + 2);

                                    String decrypted;
                                    // Parse using the same logic as legacy group format
                                    if (encryptedPart.contains("|")) {
                                        String[] segments = encryptedPart.split("\\|");
                                        if (segments.length >= 2) {
                                            String last = segments[segments.length - 1];
                                            int idx2 = last.indexOf(':');
                                            if (idx2 > 0) {
                                                String ivBase64 = last.substring(0, idx2);
                                                String encPayload = last.substring(idx2 + 1);
                                                String wrappedKeyForMe = null;
                                                for (int i = 0; i < segments.length - 1; i++) {
                                                    String seg = segments[i];
                                                    int c = seg.indexOf(':');
                                                    if (c > 0) {
                                                        String user = seg.substring(0, c);
                                                        String encKey = seg.substring(c + 1);
                                                        if (user.equals(username)) {
                                                            wrappedKeyForMe = encKey;
                                                            break;
                                                        }
                                                    }
                                                }
                                                if (wrappedKeyForMe != null) {
                                                    SecretKey aesKey = Decryption.decryptAESKeyRSA(wrappedKeyForMe,
                                                            privateKey);
                                                    byte[] ivBytes = Base64.getDecoder().decode(ivBase64);
                                                    GCMParameterSpec iv = new GCMParameterSpec(128, ivBytes);
                                                    String plainMessage = Decryption.decryptAES(encPayload, aesKey, iv);
                                                    decrypted = senderName + ": " + plainMessage;
                                                } else {
                                                    // No key for us; skip
                                                    continue;
                                                }
                                            } else {
                                                // malformed last segment; skip
                                                continue;
                                            }
                                        } else {
                                            continue;
                                        }
                                    } else if (encryptedPart.contains(":")) {
                                        String[] parts = encryptedPart.split(":", 3);
                                        if (parts.length == 3) {
                                            String encryptedAESKey = parts[0];
                                            String ivBase64 = parts[1];
                                            String encryptedMessage = parts[2];
                                            SecretKey aesKey = Decryption.decryptAESKeyRSA(encryptedAESKey, privateKey);
                                            byte[] ivBytes = Base64.getDecoder().decode(ivBase64);
                                            GCMParameterSpec iv = new GCMParameterSpec(128, ivBytes);
                                            String plainMessage = Decryption.decryptAES(encryptedMessage, aesKey, iv);
                                            decrypted = senderName + ": " + plainMessage;
                                        } else {
                                            continue;
                                        }
                                    } else {
                                        String plainMessage = Decryption.decryptRSA(encryptedPart, privateKey);
                                        decrypted = senderName + ": " + plainMessage;
                                    }

                                    String finalDecrypted = decrypted;
                                    Platform.runLater(() -> controller.appendGroupMessage(finalDecrypted));
                                }
                            }
                        }
                    } catch (Exception ex) {
                        System.err.println("Failed to process group message: " + ex.getMessage());
                    }
                    continue;
                }
                if (line.startsWith("/groups ")) {
                    String groupsData = line.substring(8);
                    if (!groupsData.isEmpty()) {
                        String[] groupEntries = groupsData.split("\\|");
                        for (String entry : groupEntries) {
                            String[] parts = entry.split(":", 3);
                            if (parts.length == 3) {
                                String groupId = parts[0];
                                String groupName = parts[1];
                                Group group = groups.get(groupId);
                                if (group == null) {
                                    group = new Group(groupId, groupName, "", new ArrayList<>(),
                                            System.currentTimeMillis());
                                    groups.put(groupId, group);
                                }
                                // Request members to get up-to-date counts
                                if (out != null) {
                                    out.println("/groupmembers " + groupId);
                                }
                            }
                        }
                        if (controller != null) {
                            Platform.runLater(() -> controller.refreshGroups(new ArrayList<>(groups.values())));
                        }
                    }
                    continue;
                }
                if (line.startsWith("/members ")) {
                    String[] parts = line.substring(9).split(" ", 2);
                    String groupId = parts[0];
                    String[] members = parts.length > 1 && !parts[1].isEmpty() ? parts[1].split("\\|") : new String[0];
                    Group group = groups.get(groupId);
                    if (group != null) {
                        group.setMembers(Arrays.asList(members));
                        // If this is the currently selected group, update UI header and member drawer
                        if (controller != null && controller.getCurrentGroup() != null
                                && groupId.equals(controller.getCurrentGroup().getGroupId())) {
                            Group finalGroup = group;
                            Platform.runLater(() -> controller.onGroupMembersUpdated(finalGroup));
                        }
                        // Refresh groups list to update counts
                        if (controller != null) {
                            Platform.runLater(() -> controller.refreshGroups(new ArrayList<>(groups.values())));
                        }
                    }
                    continue;
                }
                if (line.startsWith("/users ")) {
                    String usersData = line.substring(7);
                    System.out.println("Received /users command with data: '" + usersData + "'");
                    String[] users = usersData.split("\\|");
                    System.out.println("Parsed " + users.length + " users: " + Arrays.toString(users));
                    if (controller != null) {
                        Platform.runLater(() -> controller.updateOnlineUsers(Arrays.asList(users)));
                    }
                    continue;
                }
                if (line.startsWith("/groupupdated ") || line.startsWith("/groupmemberadded ")) {
                    // Refresh groups
                    out.println("/listgroups");
                    continue;
                }

                final String decryptedMessage;

                // Check if it's a system message (no decryption needed)
                if (line.startsWith("Welcome ") ||
                        line.endsWith(" joined the chat!") ||
                        line.endsWith(" left the chat!")) {
                    decryptedMessage = line;
                }
                // Check if it's a group message
                else if (line.startsWith("[") && line.contains("]: ")) {
                    int colonIndex = line.indexOf(": ");
                    String senderName = line.substring(1, line.indexOf(']')); // strip brackets
                    String encryptedPart = line.substring(colonIndex + 2);

                    try {
                        // Preferred format (group): user1:encKey1|user2:encKey2|...|ivBase64:encPayload
                        if (encryptedPart.contains("|")) {
                            String[] segments = encryptedPart.split("\\|");
                            if (segments.length >= 2) {
                                // Last segment must be ivBase64:encryptedMessage
                                String last = segments[segments.length - 1];
                                int idx = last.indexOf(':');
                                if (idx > 0) {
                                    String ivBase64 = last.substring(0, idx);
                                    String encPayload = last.substring(idx + 1);

                                    // Find this client's wrapped AES key
                                    String wrappedKeyForMe = null;
                                    for (int i = 0; i < segments.length - 1; i++) {
                                        String seg = segments[i];
                                        int c = seg.indexOf(':');
                                        if (c > 0) {
                                            String user = seg.substring(0, c);
                                            String encKey = seg.substring(c + 1);
                                            if (user.equals(username)) {
                                                wrappedKeyForMe = encKey;
                                                break;
                                            }
                                        }
                                    }

                                    if (wrappedKeyForMe != null) {
                                        SecretKey aesKey = Decryption.decryptAESKeyRSA(wrappedKeyForMe, privateKey);
                                        byte[] ivBytes = Base64.getDecoder().decode(ivBase64);
                                        GCMParameterSpec iv = new GCMParameterSpec(128, ivBytes);
                                        String plainMessage = Decryption.decryptAES(encPayload, aesKey, iv);
                                        decryptedMessage = senderName + ": " + plainMessage;
                                    } else {
                                        // No key for us; cannot decrypt — skip message silently
                                        System.err
                                                .println("No wrapped key for user '" + username + "' in group message");
                                        continue;
                                    }
                                } else {
                                    // Malformed last segment; fallback to generic handling below
                                    // attempt old 3-part format
                                    String[] parts = encryptedPart.split(":", 3);
                                    if (parts.length == 3) {
                                        String encryptedAESKey = parts[0];
                                        String ivBase64 = parts[1];
                                        String encryptedMessage = parts[2];
                                        SecretKey aesKey = Decryption.decryptAESKeyRSA(encryptedAESKey, privateKey);
                                        byte[] ivBytes = Base64.getDecoder().decode(ivBase64);
                                        GCMParameterSpec iv = new GCMParameterSpec(128, ivBytes);
                                        String plainMessage = Decryption.decryptAES(encryptedMessage, aesKey, iv);
                                        decryptedMessage = senderName + ": " + plainMessage;
                                    } else {
                                        String plainMessage = Decryption.decryptRSA(encryptedPart, privateKey);
                                        decryptedMessage = senderName + ": " + plainMessage;
                                    }
                                }
                            } else {
                                // Not enough segments; fallback
                                String plainMessage = Decryption.decryptRSA(encryptedPart, privateKey);
                                decryptedMessage = senderName + ": " + plainMessage;
                            }
                        } else if (encryptedPart.contains(":")) {
                            // Legacy 3-part AES format: encKey:iv:cipher
                            String[] parts = encryptedPart.split(":", 3);
                            if (parts.length == 3) {
                                String encryptedAESKey = parts[0];
                                String ivBase64 = parts[1];
                                String encryptedMessage = parts[2];
                                SecretKey aesKey = Decryption.decryptAESKeyRSA(encryptedAESKey, privateKey);
                                byte[] ivBytes = Base64.getDecoder().decode(ivBase64);
                                GCMParameterSpec iv = new GCMParameterSpec(128, ivBytes);
                                String plainMessage = Decryption.decryptAES(encryptedMessage, aesKey, iv);
                                decryptedMessage = senderName + ": " + plainMessage;
                            } else {
                                String plainMessage = Decryption.decryptRSA(encryptedPart, privateKey);
                                decryptedMessage = senderName + ": " + plainMessage;
                            }
                        } else {
                            // Fallback to simple RSA decryption
                            String plainMessage = Decryption.decryptRSA(encryptedPart, privateKey);
                            decryptedMessage = senderName + ": " + plainMessage;
                        }
                    } catch (Exception e) {
                        System.err.println("Failed to decrypt group message: " + e.getMessage());
                        System.err.println("Message was: " + line.substring(0, Math.min(80, line.length())) + "...");
                        continue;
                    }
                } else {
                    // Private or broadcast message: expected format "Sender: <payload>"
                    int idx = line.indexOf(": ");
                    if (idx > 0) {
                        String senderName = line.substring(0, idx);
                        String payload = line.substring(idx + 2);
                        try {
                            if (payload.contains(":")) {
                                String[] parts = payload.split(":", 3);
                                if (parts.length == 3) {
                                    // AES-GCM payload with RSA-wrapped AES key
                                    String encryptedAESKey = parts[0];
                                    String ivBase64 = parts[1];
                                    String encryptedMessage = parts[2];
                                    SecretKey aesKey = Decryption.decryptAESKeyRSA(encryptedAESKey, privateKey);
                                    byte[] ivBytes = Base64.getDecoder().decode(ivBase64);
                                    GCMParameterSpec iv = new GCMParameterSpec(128, ivBytes);
                                    String plainMessage = Decryption.decryptAES(encryptedMessage, aesKey, iv);
                                    decryptedMessage = senderName + ": " + plainMessage;
                                } else {
                                    // Fallback: RSA-only encryption of whole payload
                                    String plainMessage = Decryption.decryptRSA(payload, privateKey);
                                    decryptedMessage = senderName + ": " + plainMessage;
                                }
                            } else {
                                // Not encrypted or unknown format; pass through
                                decryptedMessage = line;
                            }
                        } catch (Exception e) {
                            System.err.println("Failed to decrypt private message: " + e.getMessage());
                            System.err
                                    .println("Message was: " + line.substring(0, Math.min(80, line.length())) + "...");
                            // Skip message on decryption failure
                            continue;
                        }
                    } else {
                        // No sender separator — pass through (system or malformed)
                        decryptedMessage = line;
                    }
                }

                if ("/quit".equals(decryptedMessage)) {
                    break;
                }
                if (controller != null) {
                    Platform.runLater(() -> controller.receiveMessage(decryptedMessage));
                }
            }

        } catch (IOException | InterruptedException e) {
            System.out.println("Client connection lost.");
            e.printStackTrace();
        } finally {
            shutdown();
        }
    }

    private void shutdown() {
        if (loggedIn && username != null && !username.isBlank() && out != null) {
            out.println("/quit");
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        listening = false;
        loggedIn = false;
        try {
            if (client != null && !client.isClosed())
                client.close();
            if (in != null)
                in.close();
            if (out != null)
                out.close();
        } catch (IOException e) {
            // ignore
        } finally {
            IChatController ctrl = controller;
            if (ctrl != null) {
                Platform.runLater(() -> ctrl.setConnectionStatus(false));
            }
        }
    }

    public void disconnect() {
        shutdown();
    }

    public void setLoggedIn(boolean loggedIn) {
        this.loggedIn = loggedIn;
    }

    public void setController(IChatController controller) {
        this.controller = controller;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    public PrintWriter getOut() {
        return out;
    }

    public BufferedReader getIn() {
        return in;
    }

    public PublicKey getPublicKeyForUser(String username) {
        return publicKeyCache.get(username);
    }

    // Group management methods
    public void createGroup(String groupName) {
        if (out != null) {
            out.println("/creategroup " + groupName);
        }
    }

    public void requestGroups() {
        if (out != null) {
            out.println("/listgroups");
        }
    }

    public void addToGroup(String groupId, String username) {
        if (out != null) {
            out.println("/addtogroup " + groupId + " " + username);
        }
    }

    public void requestGroupMembers(String groupId) {
        if (out != null) {
            out.println("/groupmembers " + groupId);
        }
    }

    public void requestOnlineUsers() {
        if (out != null) {
            out.println("/getusers");
        }
    }

    public void sendGroupMessage(String groupId, String message) {
        if (out != null) {
            out.println("/groupmsg " + groupId + " " + message);
        }
    }

    public Group getGroup(String groupId) {
        return groups.get(groupId);
    }

    public Collection<Group> getAllGroups() {
        return new ArrayList<>(groups.values());
    }
}
