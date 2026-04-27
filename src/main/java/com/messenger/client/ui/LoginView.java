package com.messenger.client.ui;

import com.messenger.client.ChatManager;
import com.messenger.client.ClientMain;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class LoginView {
    private ClientMain mainApp;
    private ChatManager chatManager;
    private StackPane root;

    private TextField usernameField;
    private PasswordField passwordField;
    private Label errorLabel;

    private TextField regUsernameField;
    private TextField regPhoneField;
    private PasswordField regPasswordField;
    private Label regErrorLabel;

    public LoginView(ClientMain mainApp, ChatManager chatManager) {
        this.mainApp = mainApp;
        this.chatManager = chatManager;
        this.root = new StackPane();
        switchToLogin();
    }

    public Pane getView() {
        return root;
    }

    private void switchToLogin() {
        root.getChildren().clear();
        root.getChildren().add(buildLogin());
    }

    private void switchToRegister() {
        root.getChildren().clear();
        root.getChildren().add(buildRegister());
    }

    private StackPane buildLogin() {
        Label title = new Label("Messenger 💬");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        title.setTextFill(Color.WHITE);

        Label subtitle = new Label("Connectez-vous pour accéder à vos messages");
        subtitle.setFont(Font.font("Arial", 11));
        subtitle.setTextFill(Color.web("#d0eeff"));
        subtitle.setWrapText(true);
        subtitle.setMaxWidth(300);

        Label userLabel = new Label("Nom d'utilisateur");
        userLabel.setFont(Font.font("Arial", 12));
        userLabel.setTextFill(Color.web("#d0eeff"));

        usernameField = new TextField();
        usernameField.setMaxWidth(320);
        usernameField.setPrefHeight(48);
        usernameField.setStyle(fieldStyle());

        Label passLabel = new Label("Mot de passe");
        passLabel.setFont(Font.font("Arial", 12));
        passLabel.setTextFill(Color.web("#d0eeff"));

        passwordField = new PasswordField();
        passwordField.setPromptText("Mot de passe");
        passwordField.setMaxWidth(320);
        passwordField.setPrefHeight(48);
        passwordField.setStyle(fieldStyle());

        errorLabel = new Label("");
        errorLabel.setTextFill(Color.web("#ff6b6b"));
        errorLabel.setFont(Font.font("Arial", 11));
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(300);

        Button loginBtn = new Button("Se connecter");
        loginBtn.setMaxWidth(320);
        loginBtn.setPrefHeight(48);
        loginBtn.setStyle(btnStyle("#1A2A4A"));
        loginBtn.setOnMouseEntered(e -> loginBtn.setStyle(btnStyle("#253D68")));
        loginBtn.setOnMouseExited(e -> loginBtn.setStyle(btnStyle("#1A2A4A")));

        loginBtn.setOnAction(e -> {
            errorLabel.setText("Connecting for Login...");
            chatManager.setOnLoginSuccess(() -> {
                mainApp.navigateToChat();
            });
            chatManager.setOnLoginFailed(() -> {
                errorLabel.setText("Login failed! Invalid credentials.");
            });
            chatManager.connectAndAuth("127.0.0.1", 1234, usernameField.getText(), passwordField.getText(), "", false);
        });

        Label signupLink = new Label("Pas encore de compte ? S'inscrire");
        signupLink.setFont(Font.font("Arial", 12));
        signupLink.setTextFill(Color.web("#4fc3f7"));
        signupLink.setStyle("-fx-cursor: hand;");
        signupLink.setOnMouseEntered(e -> signupLink.setStyle("-fx-cursor: hand; -fx-underline: true;"));
        signupLink.setOnMouseExited(e -> signupLink.setStyle("-fx-cursor: hand;"));
        signupLink.setOnMouseClicked(e -> switchToRegister());

        VBox card = new VBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxWidth(360);
        card.setPadding(new Insets(40, 36, 40, 36));
        card.setStyle("-fx-background-color: #2a5081; -fx-background-radius: 20;");
        card.getChildren().addAll(title, subtitle, userLabel, usernameField, passLabel, passwordField, errorLabel,
                loginBtn, signupLink);

        StackPane pane = new StackPane(card);
        pane.setStyle("-fx-background-color: #87CEEB;");
        pane.setPadding(new Insets(30));
        return pane;
    }

    private StackPane buildRegister() {
        Label title = new Label("Messenger 💬");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        title.setTextFill(Color.WHITE);

        Label subtitle = new Label("Créez votre compte");
        subtitle.setFont(Font.font("Arial", 11));
        subtitle.setTextFill(Color.web("#d0eeff"));

        Label usernameLabel = new Label("Nom d'utilisateur");
        usernameLabel.setFont(Font.font("Arial", 12));
        usernameLabel.setTextFill(Color.web("#d0eeff"));

        regUsernameField = new TextField();
        regUsernameField.setPromptText("Nom d'utilisateur");
        regUsernameField.setMaxWidth(320);
        regUsernameField.setPrefHeight(48);
        regUsernameField.setStyle(fieldStyle());

        Label phoneLabel = new Label("Numéro de téléphone");
        phoneLabel.setFont(Font.font("Arial", 12));
        phoneLabel.setTextFill(Color.web("#d0eeff"));

        regPhoneField = new TextField();
        regPhoneField.setPromptText("+212");
        regPhoneField.setMaxWidth(320);
        regPhoneField.setPrefHeight(48);
        regPhoneField.setStyle(fieldStyle());

        Label passLabel = new Label("Mot de passe");
        passLabel.setFont(Font.font("Arial", 12));
        passLabel.setTextFill(Color.web("#d0eeff"));

        regPasswordField = new PasswordField();
        regPasswordField.setPromptText("Min 4 caractères");
        regPasswordField.setMaxWidth(320);
        regPasswordField.setPrefHeight(48);
        regPasswordField.setStyle(fieldStyle());

        regErrorLabel = new Label("");
        regErrorLabel.setTextFill(Color.web("#ff6b6b"));
        regErrorLabel.setFont(Font.font("Arial", 11));
        regErrorLabel.setWrapText(true);
        regErrorLabel.setMaxWidth(300);

        Button registerBtn = new Button("Créer mon compte");
        registerBtn.setMaxWidth(320);
        registerBtn.setPrefHeight(48);
        registerBtn.setStyle(btnStyle("#1A2A4A"));

        registerBtn.setOnAction(e -> {
            regErrorLabel.setText("Connecting for Registration...");
            chatManager.setOnLoginSuccess(() -> {
                mainApp.navigateToChat();
            });
            chatManager.setOnLoginFailed(() -> {
                regErrorLabel.setText("Registration failed!");
            });
            chatManager.connectAndAuth("127.0.0.1", 1234, regUsernameField.getText(), regPasswordField.getText(),
                    regPhoneField.getText(), true);
        });

        Label loginLink = new Label("Déjà un compte ? Se connecter");
        loginLink.setFont(Font.font("Arial", 12));
        loginLink.setTextFill(Color.web("#4fc3f7"));
        loginLink.setStyle("-fx-cursor: hand;");
        loginLink.setOnMouseEntered(e -> loginLink.setStyle("-fx-cursor: hand; -fx-underline: true;"));
        loginLink.setOnMouseExited(e -> loginLink.setStyle("-fx-cursor: hand;"));
        loginLink.setOnMouseClicked(e -> switchToLogin());

        VBox card = new VBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxWidth(360);
        card.setPadding(new Insets(40, 36, 40, 36));
        card.setStyle("-fx-background-color: #2a5080; -fx-background-radius: 20;");
        card.getChildren().addAll(title, subtitle, usernameLabel, regUsernameField, phoneLabel, regPhoneField,
                passLabel, regPasswordField, regErrorLabel, registerBtn, loginLink);

        StackPane pane = new StackPane(card);
        pane.setStyle("-fx-background-color: #87CEEB;");
        pane.setPadding(new Insets(30));
        return pane;
    }

    private String fieldStyle() {
        return "-fx-background-color: #1a2a4a; -fx-text-fill: white; -fx-prompt-text-fill: #888; -fx-background-radius: 12; -fx-border-color: transparent; -fx-border-radius: 12; -fx-padding: 0 14; -fx-font-size: 13;";
    }

    private String btnStyle(String color) {
        return "-fx-background-color: " + color
                + "; -fx-text-fill: white; -fx-font-size: 15; -fx-font-weight: bold; -fx-background-radius: 12; -fx-cursor: hand;";
    }
}
