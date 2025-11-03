package se.mau.chifferchat.ui;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.kordamp.bootstrapfx.BootstrapFX;

import java.io.IOException;

public class SceneManager {

    private static Stage primaryStage;

    public static void switchScene(String fxmlPath, String title) throws IOException {
        FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxmlPath));
        Scene scene = new Scene(loader.load(), 1280, 860);

        scene.getStylesheets().add(BootstrapFX.bootstrapFXStylesheet());
        scene.getStylesheets().add(SceneManager.class.getResource("/se/mau/chifferchat/styles.css").toExternalForm());

        primaryStage.setScene(scene);
        primaryStage.setTitle(title);
        primaryStage.show();
    }

    public static Stage getStage() {
        return primaryStage;
    }

    public static void setStage(Stage stage) {
        primaryStage = stage;
    }
}
