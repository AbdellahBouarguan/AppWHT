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

        LoginView loginView = new LoginView(this, chatManager);
        Scene scene = new Scene(loginView.getView(), 400, 300);
        primaryStage.setTitle("Messenger - Login");
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
