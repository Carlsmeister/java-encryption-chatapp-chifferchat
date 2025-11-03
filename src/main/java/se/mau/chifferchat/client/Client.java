package se.mau.chifferchat.client;

import javafx.application.Platform;
import se.mau.chifferchat.ui.ChatController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client implements Runnable {
    private Socket client;
    private BufferedReader in;
    private PrintWriter out;
    private volatile boolean listening = true;
    private volatile boolean loggedIn = false;

    private volatile ChatController controller;
    private volatile String username;

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

            if (controller != null) {
                Platform.runLater(() -> controller.setConnectionStatus(true));
            }

            while (!loggedIn) {
                Thread.sleep(50);
            }

            if (username != null && out != null) {
                out.println(username);
            }

            String message;
            while (listening && (message = in.readLine()) != null) {
                final String incoming = message;
                if ("/quit".equals(incoming)) {
                    break;
                }
                if (controller != null) {
                    Platform.runLater(() -> controller.receiveMessage(incoming));
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
            if (client != null && !client.isClosed()) client.close();
            if (in != null) in.close();
            if (out != null) out.close();
        } catch (IOException e) {
            // ignore
        } finally {
            ChatController ctrl = controller;
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

    public void setController(ChatController controller) {
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
}
