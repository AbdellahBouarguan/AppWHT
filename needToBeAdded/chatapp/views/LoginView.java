package com.chatapp.views;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class LoginView {

    // =============================================
    // VARIABLES LOGIN
    // =============================================
    public TextField usernameField;
    public PasswordField passwordField;
    public Button loginBtn;
    public Label errorLabel;
    public Label signupLink;
    public ProgressBar progressBar;

    // =============================================
    // VARIABLES REGISTER
    // =============================================
    public TextField regFullNameField;
    public TextField regUsernameField;
    public TextField regPhoneField;
    public PasswordField regPasswordField;
    public Label regErrorLabel;
    public Label loginLink;
    public Button registerBtn;

    // =============================================
    // SPLASH
    // =============================================
    public VBox buildSplash() {


        Label appName = new Label("TalkApp 💬");
        appName.setFont(Font.font("Arial", FontWeight.BOLD, 38));
        appName.setTextFill(Color.WHITE);

        Label subtitle = new Label("Connecte-toi avec le monde");
        subtitle.setFont(Font.font("Arial", 14));
        subtitle.setTextFill(Color.web("#e0f4ff"));

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(200);
        progressBar.setPrefHeight(6);
        progressBar.setStyle(
                "-fx-accent: white;" +
                        "-fx-background-color: rgba(255,255,255,0.3);" +
                        "-fx-background-radius: 4;" +
                        "-fx-border-radius: 4;"
        );

        Label loadingLabel = new Label("Chargement...");
        loadingLabel.setFont(Font.font("Arial", 11));
        loadingLabel.setTextFill(Color.web("#e0f4ff"));

        VBox splash = new VBox(20);
        splash.setAlignment(Pos.CENTER);
        splash.setStyle("-fx-background-color: #87CEEB;");
        splash.getChildren().addAll(
                appName,
                subtitle,
                progressBar,
                loadingLabel
        );

        return splash;
    }

    // =============================================
    // LOGIN
    // =============================================
    public StackPane buildLogin() {

        Label title = new Label("      TalkApp 💬");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        title.setTextFill(Color.WHITE);

        Label subtitle = new Label("Connectez-vous pour accéder à vos messages");
        subtitle.setFont(Font.font("Arial", 11));
        subtitle.setTextFill(Color.web("#d0eeff"));
        subtitle.setWrapText(true);
        subtitle.setMaxWidth(300);

        // Username
        Label userLabel = new Label("Nom d'utilisateur");
        userLabel.setFont(Font.font("Arial", 12));
        userLabel.setTextFill(Color.web("#d0eeff"));

        usernameField = new TextField();
        usernameField.setMaxWidth(320);
        usernameField.setPrefHeight(48);
        usernameField.setStyle(fieldStyle());

        // Password
        Label passLabel = new Label("Mot de passe");
        passLabel.setFont(Font.font("Arial", 12));
        passLabel.setTextFill(Color.web("#d0eeff"));

        passwordField = new PasswordField();
        passwordField.setPromptText("Mot de passe");
        passwordField.setMaxWidth(320);
        passwordField.setPrefHeight(48);
        passwordField.setStyle(fieldStyle());

        // Erreur
        errorLabel = new Label("");
        errorLabel.setTextFill(Color.web("#ff6b6b"));
        errorLabel.setFont(Font.font("Arial", 11));
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(300);

        // Bouton
        loginBtn = new Button("Se connecter");
        loginBtn.setMaxWidth(320);
        loginBtn.setPrefHeight(48);
        loginBtn.setStyle(btnStyle("#1A2A4A"));
        loginBtn.setOnMouseEntered(e ->
                loginBtn.setStyle(btnStyle("#1A2A4A"))
        );
        loginBtn.setOnMouseExited(e ->
                loginBtn.setStyle(btnStyle("#1A2A4A"))
        );

        // Lien inscription
        signupLink = new Label("Pas encore de compte ? S'inscrire");
        signupLink.setFont(Font.font("Arial", 12));
        signupLink.setTextFill(Color.web("#4fc3f7"));
        signupLink.setStyle("-fx-cursor: hand;");
        signupLink.setOnMouseEntered(e ->
                signupLink.setStyle("-fx-cursor: hand; -fx-underline: true;")
        );
        signupLink.setOnMouseExited(e ->
                signupLink.setStyle("-fx-cursor: hand;")
        );

        // Card
        VBox card = new VBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxWidth(360);
        card.setPadding(new Insets(40, 36, 40, 36));
        card.setStyle(
                "-fx-background-color: #2a5081;" +
                        "-fx-background-radius: 20;"
        );
        card.getChildren().addAll(
                title,
                subtitle,
                userLabel,
                usernameField,
                passLabel,
                passwordField,
                errorLabel,
                loginBtn,
                signupLink
        );

        StackPane root = new StackPane(card);
        root.setStyle("-fx-background-color: #87CEEB;");
        root.setPadding(new Insets(30));

        return root;
    }

    // =============================================
    // REGISTER
    // =============================================
    public VBox buildRegister() {

        Label title = new Label("      TalkApp 💬");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        title.setTextFill(Color.WHITE);

        Label subtitle = new Label("Créez votre compte ");
        subtitle.setFont(Font.font("Arial", 11));
        subtitle.setTextFill(Color.web("#d0eeff"));

        // Nom complet
        Label fullNameLabel = new Label("Nom complet");
        fullNameLabel.setFont(Font.font("Arial", 12));
        fullNameLabel.setTextFill(Color.web("#d0eeff"));

        regFullNameField = new TextField();
        regFullNameField.setPromptText("Nom complet");
        regFullNameField.setMaxWidth(320);
        regFullNameField.setPrefHeight(48);
        regFullNameField.setStyle(fieldStyle());

        // Nom d'utilisateur
        Label usernameLabel = new Label("Nom d'utilisateur");
        usernameLabel.setFont(Font.font("Arial", 12));
        usernameLabel.setTextFill(Color.web("#d0eeff"));

        regUsernameField = new TextField();
        regUsernameField.setPromptText("Nom d'utilisateur");
        regUsernameField.setMaxWidth(320);
        regUsernameField.setPrefHeight(48);
        regUsernameField.setStyle(fieldStyle());

        // Numéro de téléphone
        Label phoneLabel = new Label("Numéro de téléphone");
        phoneLabel.setFont(Font.font("Arial", 12));
        phoneLabel.setTextFill(Color.web("#d0eeff"));

        regPhoneField = new TextField();
        regPhoneField.setPromptText("+212");
        regPhoneField.setMaxWidth(320);
        regPhoneField.setPrefHeight(48);
        regPhoneField.setStyle(fieldStyle());

        // Mot de passe
        Label passLabel = new Label("Mot de passe");
        passLabel.setFont(Font.font("Arial", 12));
        passLabel.setTextFill(Color.web("#d0eeff"));

        regPasswordField = new PasswordField();
        regPasswordField.setPromptText("Min 4 caractères");
        regPasswordField.setMaxWidth(320);
        regPasswordField.setPrefHeight(48);
        regPasswordField.setStyle(fieldStyle());

        // Erreur
        regErrorLabel = new Label("");
        regErrorLabel.setTextFill(Color.web("#ff6b6b"));
        regErrorLabel.setFont(Font.font("Arial", 11));
        regErrorLabel.setWrapText(true);
        regErrorLabel.setMaxWidth(300);

        // Bouton
        registerBtn = new Button("Créer mon compte");
        registerBtn.setMaxWidth(320);
        registerBtn.setPrefHeight(48);
        registerBtn.setStyle(btnStyle("#1A2A4A"));

        // Lien retour login
        loginLink = new Label("Déjà un compte ? Se connecter");
        loginLink.setFont(Font.font("Arial", 12));
        loginLink.setTextFill(Color.web("#4fc3f7"));
        loginLink.setStyle("-fx-cursor: hand;");
        loginLink.setOnMouseEntered(e ->
                loginLink.setStyle("-fx-cursor: hand; -fx-underline: true;")
        );
        loginLink.setOnMouseExited(e ->
                loginLink.setStyle("-fx-cursor: hand;")
        );

        // Card
        VBox card = new VBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxWidth(360);
        card.setPadding(new Insets(40, 36, 40, 36));
        card.setStyle(
                "-fx-background-color: #2a5080;" +
                        "-fx-background-radius: 20;"
        );
        card.getChildren().addAll(
                title, subtitle,
                fullNameLabel, regFullNameField,
                usernameLabel, regUsernameField,
                phoneLabel,    regPhoneField,
                passLabel,     regPasswordField,
                regErrorLabel,
                registerBtn,
                loginLink
        );

        return card;
    }

    // =============================================
    // STYLES
    // =============================================
    private String fieldStyle() {
        return
                "-fx-background-color: #1a2a4a;" +
                        "-fx-text-fill: white;" +
                        "-fx-prompt-text-fill: #888;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: transparent;" +
                        "-fx-border-radius: 12;" +
                        "-fx-padding: 0 14;" +
                        "-fx-font-size: 13;";
    }

    public String btnStyle(String color) {
        return
                "-fx-background-color: " + color + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 15;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 12;" +
                        "-fx-cursor: hand;";
    }
}
