package se.mau.chifferchat.server;

import se.mau.chifferchat.common.Group;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable {

    private final ArrayList<ConnectionHandler> connections;
    private ServerSocket serverSocket;
    private ExecutorService threadPool;

    private final HashMap<String, String> clientPublicKeys = new HashMap<>();
    private final HashMap<String, Group> groups = new HashMap<>(); // groupId -> Group

    private boolean listening = true;

    public Server() {
        connections = new ArrayList<>();
    }

    static void main(String[] args) {
        Server server = new Server();
        Thread serverThread = new Thread(server);
        serverThread.start();
        serverThread.setName("Server-Thread");
    }

    @Override
    public void run() {
        try {
            System.out.println("Server started");
            System.out.println("Waiting for connections");

            serverSocket = new ServerSocket(5090);
            threadPool = Executors.newCachedThreadPool();

            while (listening) {
                Socket client = serverSocket.accept();
                System.out.println("Client " + client.getInetAddress() + " connected");

                ConnectionHandler connectionHandler = new ConnectionHandler(this, client);
                connections.add(connectionHandler);
                threadPool.execute(connectionHandler);
            }

        } catch (IOException e) {
            shutDown();
        }
    }

    public synchronized void broadcastMessage(String message, ConnectionHandler sender) {
        for (ConnectionHandler client : connections) {
            if (client != sender && client != null) {
                client.sendMessage(message);
            }
        }
    }

    private void shutDown() {

        try {
            listening = false;
            if (threadPool != null) threadPool.shutdown();
            if (!serverSocket.isClosed()) serverSocket.close();

            for (ConnectionHandler connection : connections) {
                if (connection != null) {
                    connection.shutdown();
                }
            }

        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void removeConnection(ConnectionHandler connection) {
        connections.remove(connection);
    }

    public String getPublicKey(String username) {
        return clientPublicKeys.get(username);
    }

    public synchronized Group createGroup(String groupName, String creator) {
        Group group = new Group(groupName, creator);
        groups.put(group.getGroupId(), group);
        System.out.println("Group created: " + groupName + " by " + creator);
        return group;
    }

    public synchronized Group getGroup(String groupId) {
        return groups.get(groupId);
    }

    public synchronized List<Group> getAllGroups() {
        return new ArrayList<>(groups.values());
    }

    public synchronized List<Group> getGroupsForUser(String username) {
        List<Group> userGroups = new ArrayList<>();
        for (Group group : groups.values()) {
            if (group.hasMember(username)) {
                userGroups.add(group);
            }
        }
        return userGroups;
    }

    public synchronized boolean addMemberToGroup(String groupId, String username) {
        Group group = groups.get(groupId);
        if (group != null) {
            group.addMember(username);
            System.out.println("Added " + username + " to group " + group.getGroupName());
            return true;
        }
        return false;
    }

    public synchronized boolean removeMemberFromGroup(String groupId, String username) {
        Group group = groups.get(groupId);
        if (group != null) {
            group.removeMember(username);
            System.out.println("Removed " + username + " from group " + group.getGroupName());
            return true;
        }
        return false;
    }

    public synchronized void broadcastToGroup(String groupId, String message, ConnectionHandler sender) {
        Group group = groups.get(groupId);
        if (group != null) {
            for (ConnectionHandler client : connections) {
                if (client != null && client != sender) {
                    String clientUsername = client.getClientUsername();
                    if (group.hasMember(clientUsername)) {
                        client.sendMessage(message);
                    }
                }
            }
        }
    }

    public List<String> getOnlineUsers() {
        List<String> users = new ArrayList<>();
        for (ConnectionHandler conn : connections) {
            if (conn != null && conn.getClientUsername() != null) {
                users.add(conn.getClientUsername());
            }
        }
        return users;
    }

    public void addPublicKey(String username, String publicKey) {
        clientPublicKeys.put(username, publicKey);
    }
}
