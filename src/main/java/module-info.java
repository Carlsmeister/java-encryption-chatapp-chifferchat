module se.mau.chifferchat {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires org.kordamp.bootstrapfx.core;


    exports se.mau.chifferchat.ui;
    //exports se.mau.chifferchat.client;
    //exports se.mau.chifferchat.server;
    //exports se.mau.chifferchat.encryption;
    opens se.mau.chifferchat.ui to javafx.fxml;
}