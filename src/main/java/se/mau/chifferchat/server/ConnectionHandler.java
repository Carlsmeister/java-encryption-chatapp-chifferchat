package se.mau.chifferchat.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ConnectionHandler implements Runnable {

    private final Server server;
    private final Socket client;
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

            String userName = in.readLine();

            out.println("Welcome " + userName + "!");
            System.out.println(userName + " connected");
            server.broadcastMessage(userName + " joined the chat!", this);
            String message;

            while ((message = in.readLine()) != null) {
                if (message.startsWith("/quit")) {
                    server.broadcastMessage(userName + " left the chat!", this);
                    System.out.println(userName + " left the chat!");
                    shutdown();
                } else {
                    server.broadcastMessage(userName + ": " + message, this);
                }

            }
        } catch (IOException e) {
            shutdown();
        }
    }

    public void sendMessage(String message) {
        out.println(message);
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
