package com.messenger.client.ui;

import com.messenger.client.ChatManager;
import com.messenger.client.ClientMain;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class LoginView {

    private ClientMain mainApp;
    private ChatManager chatManager;
    private StackPane root;

    private TextField usernameField;
    private PasswordField passwordField;
    private Label errorLabel;
    private TextField serverIpField;

    private TextField regUsernameField;
    private TextField regPhoneField;
    private PasswordField regPasswordField;
    private PasswordField regPasswordConfirmField;
    private Label regErrorLabel;
    private TextField regServerIpField;

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
        root.getChildren().add(buildLayout(buildLoginForm(), "WhatsApp"));
    }

    private void switchToRegister() {
        root.getChildren().clear();
        root.getChildren().add(buildLayout(buildRegisterForm(), "WhatsApp"));
    }

    // ===================== LAYOUT CENTRÉ =====================
    private StackPane buildLayout(VBox form, String titleText) {

        VBox background = new VBox();

        Region topHeader = new Region();
        topHeader.setPrefHeight(220);
        topHeader.setStyle("-fx-background-color: #00a884;");

        Region bottomBg = new Region();
        VBox.setVgrow(bottomBg, Priority.ALWAYS);
        bottomBg.setStyle("-fx-background-color: #111b21;");

        background.getChildren().addAll(topHeader, bottomBg);

        VBox content = new VBox(15);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(40));

        content.getChildren().addAll(
                createWhatsappLogo(),

                new Label("WhatsApp") {
                    {
                        setFont(Font.font("Segoe UI", FontWeight.BOLD, 34));
                        setTextFill(Color.web("#e9edef"));
                    }
                },

                new Label("Connectez-vous pour accéder à vos messages") {
                    {
                        setFont(Font.font("Segoe UI", 16));
                        setTextFill(Color.web("#8696a0"));
                    }
                },

                form);

        StackPane card = new StackPane(content);

        card.setMaxWidth(420);
        card.setStyle(
                "-fx-background-color: #202c33;" +
                        "-fx-background-radius: 12;");

        DropShadow shadow = new DropShadow();
        shadow.setRadius(18);
        shadow.setOffsetY(5);
        shadow.setColor(Color.rgb(0, 0, 0, 0.35));
        card.setEffect(shadow);

        StackPane mainLayout = new StackPane(background, card);
        StackPane.setAlignment(card, Pos.CENTER);

        return mainLayout;
    }

    // ===================== LOGIN =====================
    private VBox buildLoginForm() {

        VBox form = new VBox(10);
        form.setAlignment(Pos.CENTER);

        Label userLabel = label("Nom utilisateur");
        usernameField = createStyledTextField("Username");

        Label passLabel = label("Mot de passe");
        passwordField = createStyledPasswordField("Password");

        Label ipLabel = label("Adresse IP du serveur");
        serverIpField = createStyledTextField("127.0.0.1");
        serverIpField.setText("127.0.0.1");

        errorLabel = new Label("");
        errorLabel.setTextFill(Color.web("#f15c6d"));

        Button loginBtn = createPrimaryButton("Se connecter");

        loginBtn.setOnAction(e -> {

            errorLabel.setText("Connexion...");

            chatManager.setOnLoginSuccess(() -> mainApp.navigateToChat());

            chatManager.setOnLoginFailed(() -> errorLabel.setText("Echec connexion"));

            String ip = serverIpField.getText().trim();
            if (ip.isEmpty())
                ip = "127.0.0.1";

            chatManager.connectAndAuth(
                    ip,
                    1234,
                    usernameField.getText(),
                    passwordField.getText(),
                    "",
                    false);
        });

        Label link = createLinkText(" pas encore de compte? S'inscrire");
        link.setOnMouseClicked(e -> switchToRegister());

        form.getChildren().addAll(
                userLabel, usernameField,
                passLabel, passwordField,
                ipLabel, serverIpField,
                errorLabel,
                loginBtn,
                link);

        return form;
    }

    // ===================== REGISTER =====================
    private VBox buildRegisterForm() {

        VBox form = new VBox(10);
        form.setAlignment(Pos.CENTER);

        Label userLabel = label("Nom utilisateur");
        regUsernameField = createStyledTextField("Username");

        Label phoneLabel = label("Numéro téléphone");
        regPhoneField = createStyledTextField("Phone Number");

        Label passLabel = label("Mot de passe");
        regPasswordField = createStyledPasswordField("Password");

        Label pass2Label = label("Confirmer mot de passe");
        regPasswordConfirmField = createStyledPasswordField("Repeat Password");

        Label ipLabel = label("Adresse IP du serveur");
        regServerIpField = createStyledTextField("127.0.0.1");
        regServerIpField.setText("127.0.0.1");

        regErrorLabel = new Label("");
        regErrorLabel.setTextFill(Color.web("#f15c6d"));

        Button regBtn = createPrimaryButton("Register");

        regBtn.setOnAction(e -> {
            // verification mot de pass
            if (!regPasswordField.getText().equals(regPasswordConfirmField.getText())) {
                regErrorLabel.setText("Les mots de passe ne correspondent pas !");
                return;
            }

            regErrorLabel.setText("Connexion...");

            chatManager.setOnLoginSuccess(() -> mainApp.navigateToChat());

            chatManager.setOnLoginFailed(() -> regErrorLabel.setText("Echec inscription"));

            String ip = regServerIpField.getText().trim();
            if (ip.isEmpty())
                ip = "127.0.0.1";

            chatManager.connectAndAuth(
                    ip,
                    1234,
                    regUsernameField.getText(),
                    regPasswordField.getText(),
                    regPhoneField.getText(),
                    true);
        });

        Label link = createLinkText("Déjà un compte ?");
        link.setOnMouseClicked(e -> switchToLogin());

        form.getChildren().addAll(
                userLabel, regUsernameField,
                phoneLabel, regPhoneField,
                passLabel, regPasswordField,
                pass2Label, regPasswordConfirmField,
                ipLabel, regServerIpField,
                regErrorLabel,
                regBtn,
                link);

        return form;
    }

    // ===================== STYLE =====================
    private TextField createStyledTextField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setPrefHeight(40);
        tf.setStyle("-fx-background-color: #2a3942; -fx-text-fill: #e9edef; -fx-prompt-text-fill: #8696a0;");
        return tf;
    }

    private PasswordField createStyledPasswordField(String prompt) {
        PasswordField pf = new PasswordField();
        pf.setPromptText(prompt);
        pf.setPrefHeight(40);
        pf.setStyle("-fx-background-color: #2a3942; -fx-text-fill: #e9edef; -fx-prompt-text-fill: #8696a0;");
        return pf;
    }

    private Button createPrimaryButton(String text) {
        Button btn = new Button(text);
        btn.setPrefHeight(40);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setStyle("-fx-background-color: #00a884; -fx-text-fill: #111b21; -fx-font-weight: bold;");
        return btn;
    }

    private Label createLinkText(String text) {
        Label l = new Label(text);
        l.setTextFill(Color.web("#53bdeb"));
        return l;
    }

    private Label label(String text) {
        Label l = new Label(text);
        l.setTextFill(Color.web("#e9edef"));
        l.setFont(Font.font("Segoe UI", 13));
        l.setMaxWidth(Double.MAX_VALUE);
        l.setAlignment(Pos.CENTER_LEFT);
        return l;
    }

    // ===================== LOGO =====================
    private StackPane createWhatsappLogo() {

        Circle circle = new Circle(30);
        circle.setFill(Color.web("#00a884"));

        SVGPath icon = new SVGPath();
        icon.setContent(
                "M37.7 31.2c-.6-.4-3.8-2-4.4-2.1-.6-.2-1-.4-1.4.3l-2 2.5c-.4.4-.8.5-1.5.2-.6-.3-2.7-1-5.1-3.2-2-1.7-3.2-3.8-3.6-4.5-.4-.6 0-1 .3-1.3l1-1.1.6-1.1c.2-.4 0-.8 0-1.1l-2-4.8c-.6-1.3-1.1-1-1.5-1.1h-1.2c-.5 0-1.2.1-1.8.8-.5.6-2.2 2.2-2.2 5.3 0 3.2 2.3 6.3 2.6 6.7.3.4 4.6 7 11 9.7l3.7 1.4c1.5.5 3 .4 4 .2 1.3-.1 3.9-1.5 4.4-3 .5-1.5.5-2.8.4-3-.2-.4-.6-.5-1.3-.8M26 47.2c-3.9 0-7.6-1-11-3l-.7-.4-8.1 2L8.4 38l-.6-.8A21.4 21.4 0 0126 4.4a21.3 21.3 0 0121.4 21.4c0 11.8-9.6 21.4-21.4 21.4M44.2 7.6a25.8 25.8 0 00-40.6 31L0 52l13.7-3.6A25.8 25.8 0 0044.3 7.5");
        icon.setFill(Color.WHITE);
        icon.setScaleX(0.8);
        icon.setScaleY(0.8);

        return new StackPane(circle, icon);
    }
}