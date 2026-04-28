package com.chatapp;

import com.chatapp.controllers.LoginController;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        LoginController login = new LoginController(stage);
        stage.setScene(login.getScene());
        stage.setTitle("ChatApp");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}