package se.mau.chifferchat.server;

import se.mau.chifferchat.common.Group;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.stream.Collectors;

public class ConnectionHandler implements Runnable {

    private final Server server;
    private final Socket client;
    private String clientUsername;
    private BufferedReader in;
    private PrintWriter out;

    public ConnectionHandler(Server server, Socket client) {
        this.server = server;
        this.client = client;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            out = new PrintWriter(client.getOutputStream(), true);

            clientUsername = in.readLine();

            out.println("Welcome " + clientUsername + "!");
            System.out.println(clientUsername + " connected");
            server.broadcastMessage(clientUsername + " joined the chat!", this);

            // Automatically send the online users list to the new client
            List<String> users = server.getOnlineUsers();
            String usersList = String.join("|", users);
            System.out.println("Sending initial online users to " + clientUsername + ": " + usersList);
            out.println("/users " + usersList);

            // Automatically send the groups list
            List<Group> groups = server.getGroupsForUser(clientUsername);
            String groupsData = groups.stream()
                    .map(g -> g.getGroupId() + ":" + g.getGroupName() + ":" + g.getMemberCount())
                    .collect(Collectors.joining("|"));
            System.out.println("Sending initial groups to " + clientUsername + ": " + groupsData);
            out.println("/groups " + groupsData);

            String message;

            while ((message = in.readLine()) != null) {

                if (message.startsWith("/pubkey ")) {
                    String keyB64 = message.substring(8).trim();
                    server.addPublicKey(clientUsername, keyB64);
                    continue;
                }
                if (message.startsWith("/getkey ")) {
                    String target = message.substring(8).trim();
                    String key = server.getPublicKey(target);
                    if (key != null) out.println("/key " + target + " " + key);
                    else out.println("/error No key for " + target);
                    continue;
                }

                // Group commands
                if (message.startsWith("/creategroup ")) {
                    String groupName = message.substring(13).trim();
                    Group group = server.createGroup(groupName, clientUsername);
                    out.println("/groupcreated " + group.getGroupId() + " " + group.getGroupName());
                    continue;
                }
                if (message.startsWith("/listgroups")) {
                    List<Group> groupsList = server.getGroupsForUser(clientUsername);
                    String groupsInfo = groupsList.stream()
                            .map(g -> g.getGroupId() + ":" + g.getGroupName() + ":" + g.getMemberCount())
                            .collect(Collectors.joining("|"));
                    out.println("/groups " + groupsInfo);
                    continue;
                }
                if (message.startsWith("/addtogroup ")) {
                    String[] parts = message.substring(12).split(" ", 2);
                    if (parts.length == 2) {
                        String groupId = parts[0];
                        String username = parts[1];
                        boolean added = server.addMemberToGroup(groupId, username);
                        if (added) {
                            out.println("/groupupdated " + groupId);
                            server.broadcastToGroup(groupId, "/groupmemberadded " + groupId + " " + username, this);
                        }
                    }
                    continue;
                }
                if (message.startsWith("/groupmembers ")) {
                    String groupId = message.substring(14).trim();
                    Group group = server.getGroup(groupId);
                    if (group != null) {
                        String members = String.join("|", group.getMembers());
                        out.println("/members " + groupId + " " + members);
                    }
                    continue;
                }
                if (message.startsWith("/getusers")) {
                    List<String> onlineUsersList = server.getOnlineUsers();
                    String onlineUsersStr = String.join("|", onlineUsersList);
                    System.out.println("Sending online users to " + clientUsername + ": " + onlineUsersStr);
                    out.println("/users " + onlineUsersStr);
                    continue;
                }
                if (message.startsWith("/groupmsg ")) {
                    int firstSpace = message.indexOf(' ', 10);
                    if (firstSpace > 0) {
                        String groupId = message.substring(10, firstSpace);
                        String groupMessage = message.substring(firstSpace + 1);
                        // Prefix with group identifier so clients can route to the correct open chat
                        server.broadcastToGroup(groupId, "/g " + groupId + " [" + clientUsername + "]: " + groupMessage, this);
                    }
                    continue;
                }
                if (message.startsWith("/quit")) {
                    server.broadcastMessage(clientUsername + " left the chat!", this);
                    System.out.println(clientUsername + " left the chat!");
                    shutdown();
                    break;
                }

                server.broadcastMessage(clientUsername + ": " + message, this);
            }
        } catch (IOException e) {
            shutdown();
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    public String getClientUsername() {
        return clientUsername;
    }

    public Socket getClient() {
        return client;
    }

    public void shutdown() {
        try {
            if (!client.isClosed()) client.close();
            if (in != null) in.close();
            if (out != null) out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            server.removeConnection(this);
        }
    }
}

