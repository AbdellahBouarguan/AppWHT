package com.messenger.client.ui;

import com.messenger.client.ChatManager;
import com.messenger.client.ClientMain;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.effect.DropShadow;

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
        root.getChildren().add(buildLayout(buildLoginForm(), "To use Messenger on your computer:"));
    }

    private void switchToRegister() {
        root.getChildren().clear();
        root.getChildren().add(buildLayout(buildRegisterForm(), "Create a new Messenger account:"));
    }

    private StackPane buildLayout(VBox form, String leftTitle) {
        // WhatsApp Web Background
        VBox background = new VBox();
        Region topHeader = new Region();
        topHeader.setPrefHeight(222);
        topHeader.setStyle("-fx-background-color: #00a884;");
        
        Region bottomBg = new Region();
        VBox.setVgrow(bottomBg, Priority.ALWAYS);
        bottomBg.setStyle("-fx-background-color: #111b21;");
        
        background.getChildren().addAll(topHeader, bottomBg);

        // Center Card
        HBox cardContent = new HBox(50);
        cardContent.setPadding(new Insets(60, 60, 60, 60));

        // Left Side: Instructions
        VBox leftSide = new VBox(20);
        Label title = new Label(leftTitle);
        title.setFont(Font.font("System", FontWeight.NORMAL, 28));
        title.setTextFill(Color.web("#e9edef"));
        
        VBox instructionsList = new VBox(15);
        instructionsList.getChildren().addAll(
            instructionItem("1.", "Open Messenger on your device"),
            instructionItem("2.", "Tap Menu on Android, or Settings on iPhone"),
            instructionItem("3.", "Tap Linked Devices and point your phone to this screen")
        );
        Label smallDisclaimer = new Label("(For now, please use the login form on the right)");
        smallDisclaimer.setTextFill(Color.web("#8696a0"));
        smallDisclaimer.setFont(Font.font("System", 12));

        leftSide.getChildren().addAll(title, instructionsList, new Region(), smallDisclaimer);
        VBox.setVgrow(leftSide, Priority.ALWAYS);

        // Right Side: Form (Mimicking the QR Code Area)
        VBox rightSide = new VBox();
        rightSide.setAlignment(Pos.CENTER);
        rightSide.setPrefWidth(300);
        rightSide.getChildren().add(form);

        cardContent.getChildren().addAll(leftSide, rightSide);

        StackPane card = new StackPane(cardContent);
        card.setMaxSize(1000, 600);
        card.setStyle("-fx-background-color: #202c33; -fx-background-radius: 3;");
        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(0, 0, 0, 0.2));
        shadow.setRadius(15);
        shadow.setOffsetY(5);
        card.setEffect(shadow);

        StackPane mainLayout = new StackPane(background, card);
        StackPane.setAlignment(card, Pos.CENTER);
        return mainLayout;
    }

    private HBox instructionItem(String num, String text) {
        Label n = new Label(num);
        n.setTextFill(Color.web("#e9edef"));
        n.setFont(Font.font("System", FontWeight.BOLD, 18));
        Label t = new Label(text);
        t.setTextFill(Color.web("#e9edef"));
        t.setFont(Font.font("System", 18));
        t.setWrapText(true);
        HBox box = new HBox(15, n, t);
        return box;
    }

    private VBox buildLoginForm() {
        VBox form = new VBox(15);
        form.setAlignment(Pos.CENTER);
        
        usernameField = createStyledTextField("Username");
        passwordField = createStyledPasswordField("Password");
        serverIpField = createStyledTextField("Server IP (127.0.0.1)");
        serverIpField.setText("127.0.0.1");

        errorLabel = new Label("");
        errorLabel.setTextFill(Color.web("#f15c6d"));
        errorLabel.setFont(Font.font("System", 13));
        errorLabel.setWrapText(true);

        Button loginBtn = createPrimaryButton("Log In");
        loginBtn.setOnAction(e -> {
            errorLabel.setText("Connecting...");
            chatManager.setOnLoginSuccess(() -> mainApp.navigateToChat());
            chatManager.setOnLoginFailed(() -> errorLabel.setText("Login failed!"));
            String ip = serverIpField.getText().trim();
            if (ip.isEmpty()) ip = "127.0.0.1";
            chatManager.connectAndAuth(ip, 1234, usernameField.getText(), passwordField.getText(), "", false);
        });

        Label link = createLinkText("Need an account? Register");
        link.setOnMouseClicked(e -> switchToRegister());

        form.getChildren().addAll(usernameField, passwordField, serverIpField, errorLabel, loginBtn, link);
        return form;
    }

    private VBox buildRegisterForm() {
        VBox form = new VBox(15);
        form.setAlignment(Pos.CENTER);

        regUsernameField = createStyledTextField("Username");
        regPhoneField = createStyledTextField("Phone Number");
        regPasswordField = createStyledPasswordField("Password");
        regServerIpField = createStyledTextField("Server IP (127.0.0.1)");
        regServerIpField.setText("127.0.0.1");

        regErrorLabel = new Label("");
        regErrorLabel.setTextFill(Color.web("#f15c6d"));
        regErrorLabel.setFont(Font.font("System", 13));
        regErrorLabel.setWrapText(true);

        Button regBtn = createPrimaryButton("Register");
        regBtn.setOnAction(e -> {
            regErrorLabel.setText("Connecting...");
            chatManager.setOnLoginSuccess(() -> mainApp.navigateToChat());
            chatManager.setOnLoginFailed(() -> regErrorLabel.setText("Registration failed!"));
            String ip = regServerIpField.getText().trim();
            if (ip.isEmpty()) ip = "127.0.0.1";
            chatManager.connectAndAuth(ip, 1234, regUsernameField.getText(), regPasswordField.getText(),
                    regPhoneField.getText(), true);
        });

        Label link = createLinkText("Have an account? Log In");
        link.setOnMouseClicked(e -> switchToLogin());

        form.getChildren().addAll(regUsernameField, regPhoneField, regPasswordField, regServerIpField, regErrorLabel, regBtn, link);
        return form;
    }

    private TextField createStyledTextField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setPrefHeight(40);
        tf.setStyle("-fx-background-color: #2a3942; -fx-text-fill: #e9edef; -fx-prompt-text-fill: #8696a0; -fx-background-radius: 6; -fx-padding: 10; -fx-font-size: 14;");
        return tf;
    }

    private PasswordField createStyledPasswordField(String prompt) {
        PasswordField pf = new PasswordField();
        pf.setPromptText(prompt);
        pf.setPrefHeight(40);
        pf.setStyle("-fx-background-color: #2a3942; -fx-text-fill: #e9edef; -fx-prompt-text-fill: #8696a0; -fx-background-radius: 6; -fx-padding: 10; -fx-font-size: 14;");
        return pf;
    }

    private Button createPrimaryButton(String text) {
        Button btn = new Button(text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setPrefHeight(40);
        btn.setStyle("-fx-background-color: #00a884; -fx-text-fill: #111b21; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 14;");
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #00bfa5; -fx-text-fill: #111b21; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 14;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: #00a884; -fx-text-fill: #111b21; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 14;"));
        return btn;
    }

    private Label createLinkText(String text) {
        Label label = new Label(text);
        label.setTextFill(Color.web("#53bdeb"));
        label.setFont(Font.font("System", 13));
        label.setStyle("-fx-cursor: hand;");
        label.setOnMouseEntered(e -> label.setStyle("-fx-cursor: hand; -fx-underline: true;"));
        label.setOnMouseExited(e -> label.setStyle("-fx-cursor: hand; -fx-underline: false;"));
        return label;
    }
}
