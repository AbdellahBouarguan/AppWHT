package com.chatapp.controllers;

import com.chatapp.views.LoginView;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

public class LoginController {

    private Stage stage;
    private LoginView view;

    public LoginController(Stage stage) {
        this.stage = stage;
        this.view = new LoginView();
    }

    // =============================================
    // SCÈNE PRINCIPALE — LOGIN
    // =============================================
    public Scene getScene() {

        view = new LoginView();

        var splash = view.buildSplash();
        var login  = view.buildLogin();
        login.setVisible(false);
        login.setOpacity(0);

        StackPane root = new StackPane(splash, login);
        root.setStyle("-fx-background-color: #87CEEB;");

        // Logique login
        view.loginBtn.setOnAction(e -> handleLogin());
        view.passwordField.setOnAction(e -> handleLogin());

        // Lien inscription → affiche Register
        view.signupLink.setOnMouseClicked(e -> showRegister());

        // Fade in splash
        FadeTransition fadeInSplash = new FadeTransition(Duration.millis(800), splash);
        fadeInSplash.setFromValue(0);
        fadeInSplash.setToValue(1);
        fadeInSplash.play();

        // Barre progression
        Timeline progress = new Timeline();
        int steps = 30;
        for (int i = 1; i <= steps; i++) {
            double val = (double) i / steps;
            KeyFrame kf = new KeyFrame(
                    Duration.millis(i * 80),
                    e -> view.progressBar.setProgress(val)
            );
            progress.getKeyFrames().add(kf);
        }

        // Splash → Login
        progress.setOnFinished(e -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), splash);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(ev -> splash.setVisible(false));
            fadeOut.play();

            login.setVisible(true);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(600), login);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();
        });

        progress.play();

        return new Scene(root, 420, 580);
    }

    // =============================================
    // LOGIQUE LOGIN
    // =============================================
    private void handleLogin() {
        String username = view.usernameField.getText().trim();
        String password = view.passwordField.getText().trim();

        // champs vides ?
        if (username.isEmpty() || password.isEmpty()) {
            view.errorLabel.setTextFill(javafx.scene.paint.Color.web("#ff6b6b"));
            view.errorLabel.setText("Remplis tous les champs !");
            return;
        }

        //  MainView sera ajouté ici plus tard
        view.errorLabel.setTextFill(javafx.scene.paint.Color.web("#4CAF50"));
        view.errorLabel.setText("✓ Connexion réussie !");

    }

    // =============================================
    // AFFICHER ÉCRAN REGISTER
    // =============================================
    private void showRegister() {

        VBox registerCard = view.buildRegister();

        StackPane root = new StackPane(registerCard);
        root.setStyle("-fx-background-color: #87CEEB;");
        root.setPadding(new Insets(30));

        // bouton créer compte
        view.registerBtn.setOnAction(e -> handleRegister());

        // lien retour login
        view.loginLink.setOnMouseClicked(e -> stage.setScene(getScene()));

        stage.setScene(new Scene(root, 420, 620));
    }

    // =============================================
    // LOGIQUE REGISTER
    // =============================================
    private void handleRegister() {

        String fullName = view.regFullNameField.getText().trim();
        String username = view.regUsernameField.getText().trim();
        String phone    = view.regPhoneField.getText().trim();
        String password = view.regPasswordField.getText().trim();

        // champs vides ?
        if (fullName.isEmpty() || username.isEmpty() ||
                phone.isEmpty()    || password.isEmpty()) {
            view.regErrorLabel.setText(" Remplis tous les champs !");
            return;
        }

        // nom trop court ?
        if (username.length() < 3) {
            view.regErrorLabel.setText("Nom d'utilisateur trop court (min 3) !");
            return;
        }

        // numéro invalide ?
        if (phone.length() < 11) {
            view.regErrorLabel.setText(" Numéro de téléphone invalide !");
            return;
        }

        // mot de passe trop court ?
        if (password.length() < 4) {
            view.regErrorLabel.setText(" Mot de passe trop court (min 4) !");
            return;
        }

        //  inscription ok → retour login avec message succès
        stage.setScene(getScene());
        view.errorLabel.setTextFill(javafx.scene.paint.Color.web("#4CAF50"));
        view.errorLabel.setText("✓ Compte créé ! Connectez-vous.");
    }
}