package se.mau.chifferchat.ui;

import javafx.application.HostServices;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import se.mau.chifferchat.client.Client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LoginController {
    @FXML
    public Label welcomeText;
    @FXML
    public TextField username;
    @FXML
    public PasswordField password;
    @FXML
    public ScrollPane newsScrollPane;
    @FXML
    public VBox newsContainer;
    @FXML
    public HBox githubLinkBox;
    private Client client;

    @FXML
    public void initialize() {
        this.client = HelloApplication.getClient();

        username.setOnAction(this::onLoginClick);
        password.setOnAction(this::onLoginClick);

        loadNews();

//        FontIcon githubIcon = new FontIcon("fab-github");
//        githubIcon.setIconSize(20);
//        githubIcon.setIconColor(javafx.scene.paint.Color.DARKORANGE);
//        githubLinkBox.getChildren().add(0, githubIcon);
    }

    private void loadNews() {
        List<String> newsItems = new ArrayList<>();

        try (InputStream is = getClass().getResourceAsStream("/news.txt")) {
            if (is != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        newsItems.add("• " + line.trim());
                    }
                }
            }
        } catch (IOException e) {
            newsItems.add("• Unable to load news");
        }

        // Reverse to show newest first
        Collections.reverse(newsItems);

        // Add each news item as a wrapped Label to VBox
        for (String item : newsItems) {
            Label newsLabel = new Label(item);
            newsLabel.setWrapText(true);
            newsLabel.setMaxWidth(210); // Fits within sidebar with padding
            newsLabel.getStyleClass().add("news-item");
            newsContainer.getChildren().add(newsLabel);
        }
    }

    @FXML
    public void onLoginClick(ActionEvent actionEvent) {

        String user = username.getText();
        String pass = password.getText();

        if ((user.equals("Carl") || user.equals("Becca") || user.equals("Calle")) && pass.equals("12345")) {
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

    @FXML
    public void onGithubClick() {
        try {
            HostServices hostServices = HelloApplication.getAppHostServices();
            if (hostServices != null) {
                hostServices.showDocument("https://github.com/Carlsmeister");
            }
        } catch (Exception e) {
            System.err.println("Failed to open GitHub link: " + e.getMessage());
        }
    }
}
