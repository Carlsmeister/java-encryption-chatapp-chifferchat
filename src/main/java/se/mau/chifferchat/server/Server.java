package se.mau.chifferchat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server implements Runnable {

    private final ArrayList<ConnectionHandler> connections;
    private ServerSocket serverSocket;
    private ExecutorService threadPool;

    private boolean listening = true;

    public Server() {
        connections = new ArrayList<>();
    }

    public static void main(String[] args) {
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
}
