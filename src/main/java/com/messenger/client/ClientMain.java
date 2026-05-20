package com.messenger.client;

import com.messenger.client.ui.LoginView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientMain extends Application {
    private ChatManager chatManager;
    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.chatManager = new ChatManager();

        com.messenger.client.ui.SplashView splashView = new com.messenger.client.ui.SplashView(() -> {
            LoginView loginView = new LoginView(this, chatManager);
            Scene loginScene = new Scene(loginView.getView(), 1000, 700);
            primaryStage.setTitle("WhatsApp Web - Login");
            primaryStage.setScene(loginScene);
        });

        Scene scene = new Scene(splashView.getView(), 1000, 700);
        primaryStage.setTitle("WhatsApp Web");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public void navigateToChat() {
        com.messenger.client.ui.ChatView chatView = new com.messenger.client.ui.ChatView(this, chatManager);
        Scene scene = new Scene(chatView.getView(), 800, 600);
        primaryStage.setTitle("Messenger - Chat (" + chatManager.getCurrentUser().getUsername() + ")");
        primaryStage.setScene(scene);
    }

    @Override
    public void stop() {
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
