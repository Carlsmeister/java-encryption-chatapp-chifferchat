package se.mau.chifferchat.ui;

import javafx.application.Application;
import javafx.stage.Stage;
import se.mau.chifferchat.client.Client;

import java.io.IOException;

public class HelloApplication extends Application {

    private static Client client;

    public static Client getClient() {
        return client;
    }

    public static void resetClient() {
        client = new Client();
    }

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws IOException {
        client = new Client();
        SceneManager.setStage(stage);
        SceneManager.switchScene("/se/mau/chifferchat/login-view.fxml", "ChifferChat – Login");
    }
}
