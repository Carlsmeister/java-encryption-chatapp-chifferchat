package se.mau.chifferchat.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import se.mau.chifferchat.client.Client;

import java.io.IOException;

public class LoginController {
    @FXML
    public Label welcomeText;
    @FXML
    public TextField username;
    @FXML
    public PasswordField password;
    private Client client;

    @FXML
    public void initialize() {
        this.client = HelloApplication.getClient();
    }

    @FXML
    public void onLoginClick(ActionEvent actionEvent) {

        String user = username.getText();
        String pass = password.getText();

        if ((user.equals("Carl") || user.equals("Becca")) && pass.equals("12345")) {
            welcomeText.setText("Logging in...");
            client.setUsername(user);
            client.setLoggedIn(true);
            client.connect();

            try {
                SceneManager.switchScene("/se/mau/chifferchat/chat-view.fxml", "ChifferChat – Chat");
            } catch (IOException e) {
                welcomeText.setText("Login failed. Try again.");
            }
        } else {
            welcomeText.setText("Incorrect username or password. Try again");
        }
    }
}
