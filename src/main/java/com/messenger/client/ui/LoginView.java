package com.messenger.client.ui;

import com.messenger.client.ChatManager;
import com.messenger.client.ClientMain;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class LoginView {
    private VBox view;

    public LoginView(ClientMain mainApp, ChatManager chatManager) {
        view = new VBox(10);
        view.setPadding(new Insets(20));

        Label hostLabel = new Label("Server IP:");
        TextField hostField = new TextField("localhost");

        Label userLabel = new Label("Username:");
        TextField userField = new TextField();

        Label passLabel = new Label("Password:");
        PasswordField passField = new PasswordField();

        Label errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: red;");

        Button loginBtn = new Button("Login / Register");
        loginBtn.setOnAction(e -> {
            errorLabel.setText("Connecting...");
            chatManager.setOnLoginSuccess(() -> {
                mainApp.navigateToChat();
            });
            chatManager.setOnLoginFailed(() -> {
                errorLabel.setText("Login failed!");
            });
            chatManager.connectAndLogin(hostField.getText(), 1234, userField.getText(), passField.getText());
        });

        view.getChildren().addAll(hostLabel, hostField, userLabel, userField, passLabel, passField, loginBtn,
                errorLabel);
    }

    public VBox getView() {
        return view;
    }
}
