package com.messenger.client.ui;

import com.messenger.client.ChatManager;
import com.messenger.client.ClientMain;
import com.messenger.common.User;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class ChatView {
    private BorderPane view;
    private ChatManager chatManager;
    private ListView<User> usersList;
    private TextArea chatArea;
    private User selectedUser;

    public ChatView(ClientMain mainApp, ChatManager chatManager) {
        this.chatManager = chatManager;
        view = new BorderPane();

        usersList = new ListView<>();
        usersList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            selectedUser = newV;
        });
        VBox leftPane = new VBox(new Label("Online Users:"), usersList);
        leftPane.setPrefWidth(200);
        view.setLeft(leftPane);

        chatArea = new TextArea();
        chatArea.setEditable(false);

        TextField inputField = new TextField();
        inputField.setPromptText("Type a message...");
        Button sendBtn = new Button("Send");
        Button callBtn = new Button("Video Call");

        sendBtn.setOnAction(e -> {
            if (selectedUser != null && !inputField.getText().isEmpty()) {
                chatManager.sendMessage(selectedUser, inputField.getText());
                chatArea.appendText("Me to " + selectedUser.getUsername() + ": " + inputField.getText() + "\n");
                inputField.clear();
            }
        });

        callBtn.setOnAction(e -> {
            if (selectedUser != null) {
                chatManager.requestCall(selectedUser, com.messenger.common.CallType.VIDEO);
                chatArea.appendText("Initiating video call to " + selectedUser.getUsername() + "...\n");
            }
        });

        HBox inputBox = new HBox(5, inputField, sendBtn, callBtn);
        VBox centerPane = new VBox(5, chatArea, inputBox);
        view.setCenter(centerPane);

        chatManager.setUserListListener(users -> {
            Platform.runLater(() -> {
                users.removeIf(u -> u.getId().equals(chatManager.getCurrentUser().getId()));
                usersList.setItems(FXCollections.observableArrayList(users));
            });
        });

        chatManager.setMessageListener(msg -> {
            Platform.runLater(() -> {
                chatArea.appendText(msg.getSender().getUsername() + ": " + msg.getContent() + "\n");
            });
        });

        chatManager.setCallListener((callerId, type) -> {
            Platform.runLater(() -> {
                chatArea.appendText("Incoming " + type + " call from " + callerId + "...\n");
            });
        });
    }

    public BorderPane getView() {
        return view;
    }
}
