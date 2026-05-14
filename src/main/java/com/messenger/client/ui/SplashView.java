package com.messenger.client.ui;

import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Duration;

public class SplashView {
    private StackPane root;

    public SplashView(Runnable onComplete) {
        root = new StackPane();
        // Match the WhatsApp dark mode background
        root.setStyle("-fx-background-color: #111b21;");

        VBox content = new VBox(40);
        content.setAlignment(Pos.CENTER);

        // WhatsApp Icon
        SVGPath waIcon = new SVGPath();
        waIcon.setContent("M37.7 31.2c-.6-.4-3.8-2-4.4-2.1-.6-.2-1-.4-1.4.3l-2 2.5c-.4.4-.8.5-1.5.2-.6-.3-2.7-1-5.1-3.2-2-1.7-3.2-3.8-3.6-4.5-.4-.6 0-1 .3-1.3l1-1.1.6-1.1c.2-.4 0-.8 0-1.1l-2-4.8c-.6-1.3-1.1-1-1.5-1.1h-1.2c-.5 0-1.2.1-1.8.8-.5.6-2.2 2.2-2.2 5.3 0 3.2 2.3 6.3 2.6 6.7.3.4 4.6 7 11 9.7l3.7 1.4c1.5.5 3 .4 4 .2 1.3-.1 3.9-1.5 4.4-3 .5-1.5.5-2.8.4-3-.2-.4-.6-.5-1.3-.8M26 47.2c-3.9 0-7.6-1-11-3l-.7-.4-8.1 2L8.4 38l-.6-.8A21.4 21.4 0 0126 4.4a21.3 21.3 0 0121.4 21.4c0 11.8-9.6 21.4-21.4 21.4M44.2 7.6a25.8 25.8 0 00-40.6 31L0 52l13.7-3.6A25.8 25.8 0 0044.3 7.5");
        waIcon.setFill(Color.web("#676f73")); // --splashscreen-startup-icon dark mode color
        waIcon.setScaleX(2.0);
        waIcon.setScaleY(2.0);

        // Spacers for layout
        javafx.scene.layout.Region spacer1 = new javafx.scene.layout.Region();
        VBox.setVgrow(spacer1, javafx.scene.layout.Priority.ALWAYS);
        
        javafx.scene.layout.Region spacer2 = new javafx.scene.layout.Region();
        VBox.setVgrow(spacer2, javafx.scene.layout.Priority.ALWAYS);

        // Progress Bar
        ProgressBar progressBar = new ProgressBar();
        progressBar.setProgress(-1.0); // Indeterminate progress
        progressBar.setPrefWidth(300);
        progressBar.setStyle("-fx-accent: #0b846d; -fx-control-inner-background: #233138; -fx-background-color: #233138; -fx-background-radius: 5px;");
        
        // Title
        Label title = new Label("WhatsApp");
        title.setTextFill(Color.web("#e9edef"));
        title.setFont(Font.font("System", FontWeight.BOLD, 22));

        // Encryption info
        HBox encryptionBox = new HBox(5);
        encryptionBox.setAlignment(Pos.CENTER);
        
        SVGPath lockIcon = new SVGPath();
        lockIcon.setContent("M6 22C5.45 22 4.97917 21.8042 4.5875 21.4125C4.19583 21.0208 4 20.55 4 20V10C4 9.45 4.19583 8.97917 4.5875 8.5875C4.97917 8.19583 5.45 8 6 8H7V6C7 4.61667 7.4875 3.4375 8.4625 2.4625C9.4375 1.4875 10.6167 1 12 1C13.3833 1 14.5625 1.4875 15.5375 2.4625C16.5125 3.4375 17 4.61667 17 6V8H18C18.55 8 19.0208 8.19583 19.4125 8.5875C19.8042 8.97917 20 9.45 20 10V20C20 20.55 19.8042 21.0208 19.4125 21.4125C19.0208 21.8042 18.55 22 18 22H6ZM6 20H18V10H6V20ZM12 17C12.55 17 13.0208 16.8042 13.4125 16.4125C13.8042 16.0208 14 15.55 14 15C14 14.45 13.8042 13.9792 13.4125 13.5875C13.0208 13.1958 12.55 13 12 13C11.45 13 10.9792 13.1958 10.5875 13.5875C10.1958 13.9792 10 14.45 10 15C10 15.55 10.1958 16.0208 10.5875 16.4125C10.9792 16.8042 11.45 17 12 17ZM9 8H15V6C15 5.16667 14.7083 4.45833 14.125 3.875C13.5417 3.29167 12.8333 3 12 3C11.1667 3 10.4583 3.29167 9.875 3.875C9.29167 4.45833 9 5.16667 9 6V8Z");
        lockIcon.setFill(Color.web("#8696a0"));
        
        Label encryptionText = new Label("End-to-end encrypted");
        encryptionText.setTextFill(Color.web("#8696a0"));
        encryptionText.setFont(Font.font("System", 12));
        
        encryptionBox.getChildren().addAll(lockIcon, encryptionText);

        VBox bottomContainer = new VBox(15);
        bottomContainer.setAlignment(Pos.CENTER);
        bottomContainer.getChildren().addAll(progressBar, title, encryptionBox);
        bottomContainer.setPadding(new javafx.geometry.Insets(0, 0, 40, 0));

        content.getChildren().addAll(spacer1, waIcon, spacer2, bottomContainer);

        root.getChildren().add(content);

        // Simulate a loading delay then transition
        PauseTransition delay = new PauseTransition(Duration.seconds(2.5));
        delay.setOnFinished(e -> {
            if (onComplete != null) onComplete.run();
        });
        delay.play();
    }

    public StackPane getView() {
        return root;
    }
}
