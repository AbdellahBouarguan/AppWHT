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

    private com.messenger.client.media.MediaCapture mediaCapture;
    private com.messenger.client.media.StreamReceiver audioReceiver;
    private com.messenger.client.media.StreamReceiver videoReceiver;
    private com.messenger.client.media.StreamSender audioSender;
    private com.messenger.client.media.StreamSender videoSender;
    private javax.sound.sampled.SourceDataLine audioOutputLine;
    private javafx.stage.Stage mediaStage;

    public ChatView(ClientMain mainApp, ChatManager chatManager) {
        this.chatManager = chatManager;
        view = new BorderPane();

        usersList = new ListView<>();
        usersList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            selectedUser = newV;
            if (selectedUser != null) {
                chatArea.clear();
                chatManager.fetchChatHistory(selectedUser);
            }
        });

        TextField addContactField = new TextField();
        addContactField.setPromptText("Numéro de téléphone");
        addContactField.setStyle(
                "-fx-background-color: #1a2a4a; -fx-text-fill: white; -fx-prompt-text-fill: #888; -fx-background-radius: 12;");
        Button addContactBtn = new Button("Add");
        addContactBtn.setStyle(
                "-fx-background-color: #4fc3f7; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 12;");
        addContactBtn.setOnAction(e -> {
            if (!addContactField.getText().trim().isEmpty()) {
                chatManager.addContact(addContactField.getText().trim());
                addContactField.clear();
            }
        });
        HBox addContactBox = new HBox(5, addContactField, addContactBtn);

        Label contactLabel = new Label("Contacts:");
        contactLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        VBox leftPane = new VBox(10, contactLabel, addContactBox, usersList);
        leftPane.setPadding(new javafx.geometry.Insets(10));
        leftPane.setStyle("-fx-background-color: #2a5081;");
        leftPane.setPrefWidth(250);
        view.setLeft(leftPane);

        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setStyle("-fx-control-inner-background: #e0f4ff;");

        TextField inputField = new TextField();
        inputField.setPromptText("Type a message...");
        inputField.setStyle("-fx-background-color: white; -fx-background-radius: 12;");
        Button sendBtn = new Button("Send");
        sendBtn.setStyle("-fx-background-color: #1A2A4A; -fx-text-fill: white; -fx-background-radius: 12;");
        Button audioCallBtn = new Button("📞 Audio");
        Button videoCallBtn = new Button("📹 Video");
        Button endCallBtn = new Button("End Call");

        sendBtn.setOnAction(e -> {
            if (selectedUser != null && !inputField.getText().isEmpty()) {
                chatManager.sendMessage(selectedUser, inputField.getText());
                chatArea.appendText("Me to " + selectedUser.getUsername() + ": " + inputField.getText() + "\n");
                inputField.clear();
            }
        });

        audioCallBtn.setOnAction(e -> {
            if (selectedUser != null) {
                if (!selectedUser.isOnline()) {
                    chatArea.appendText("Cannot call offline user: " + selectedUser.getUsername() + "\n");
                    return;
                }
                chatManager.requestCall(selectedUser, com.messenger.common.CallType.AUDIO);
                chatArea.appendText("Initiating audio call to " + selectedUser.getUsername() + "...\n");
            }
        });

        videoCallBtn.setOnAction(e -> {
            if (selectedUser != null) {
                if (!selectedUser.isOnline()) {
                    chatArea.appendText("Cannot call offline user: " + selectedUser.getUsername() + "\n");
                    return;
                }
                chatManager.requestCall(selectedUser, com.messenger.common.CallType.VIDEO);
                chatArea.appendText("Initiating video call to " + selectedUser.getUsername() + "...\n");
            }
        });

        endCallBtn.setOnAction(e -> {
            if (selectedUser != null) {
                chatManager.endCall(selectedUser);
                stopMediaResources();
                chatArea.appendText("Ended call with " + selectedUser.getUsername() + "\n");
            }
        });

        HBox inputBox = new HBox(10, inputField, sendBtn, audioCallBtn, videoCallBtn, endCallBtn);
        inputBox.setPadding(new javafx.geometry.Insets(10));
        inputBox.setStyle("-fx-background-color: #87CEEB;");
        VBox centerPane = new VBox(0, chatArea, inputBox);
        VBox.setVgrow(chatArea, javafx.scene.layout.Priority.ALWAYS);
        view.setCenter(centerPane);

        chatManager.setUserListListener(users -> {
            Platform.runLater(() -> {
                users.removeIf(u -> u.getId().equals(chatManager.getCurrentUser().getId()));
                usersList.setItems(FXCollections.observableArrayList(users));
            });
        });

        chatManager.setStatusUpdateListener(updatedUser -> {
            Platform.runLater(() -> {
                java.util.List<User> items = new java.util.ArrayList<>(usersList.getItems());
                for (int i = 0; i < items.size(); i++) {
                    if (items.get(i).getId().equals(updatedUser.getId())) {
                        items.get(i).setOnline(updatedUser.isOnline());
                        break;
                    }
                }
                usersList.setItems(FXCollections.observableArrayList(items));
                usersList.refresh();
            });
        });

        chatManager.setMessageListener(msg -> {
            Platform.runLater(() -> {
                chatArea.appendText(msg.getSender().getUsername() + ": " + msg.getContent() + "\n");
            });
        });

        chatManager.setChatHistoryListener(history -> {
            Platform.runLater(() -> {
                chatArea.clear();
                for (com.messenger.common.Message m : history) {
                    chatArea.appendText(m.getSender().getUsername() + ": " + m.getContent() + "\n");
                }
            });
        });

        chatManager.setCallListener(new ChatManager.CallListener() {
            @Override
            public void onIncomingCall(String callerId, com.messenger.common.CallType type) {
                Platform.runLater(() -> {
                    chatArea.appendText("Incoming " + type + " call from " + callerId + "...\n");
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("Incoming Call");
                    alert.setHeaderText("Incoming " + type + " call from " + callerId);
                    alert.setContentText("Do you want to accept?");
                    ButtonType acceptBtn = new ButtonType("Accept");
                    ButtonType rejectBtn = new ButtonType("Reject", ButtonBar.ButtonData.CANCEL_CLOSE);
                    alert.getButtonTypes().setAll(acceptBtn, rejectBtn);

                    alert.showAndWait().ifPresent(result -> {
                        if (result == acceptBtn) {
                            chatManager.acceptCall(callerId, type);
                            chatArea.appendText("Call accepted. Media starting...\n");

                            String peerIp = "127.0.0.1";
                            if (chatManager.getLatestCallData() != null && chatManager.getLatestCallData().length > 2) {
                                peerIp = (String) chatManager.getLatestCallData()[2];
                            }

                            startMediaPresentation(type, 5001, 5002, peerIp);
                        } else {
                            chatManager.rejectCall(callerId, type);
                            chatArea.appendText("Call rejected.\n");
                        }
                    });
                });
            }

            @Override
            public void onCallAccepted(String calleeId, com.messenger.common.CallType type) {
                Platform.runLater(() -> {
                    chatArea.appendText(calleeId + " accepted your " + type + " call. Media starting...\n");

                    String peerIp = "127.0.0.1";
                    if (chatManager.getLatestCallData() != null && chatManager.getLatestCallData().length > 2) {
                        peerIp = (String) chatManager.getLatestCallData()[2];
                    }

                    startMediaPresentation(type, 5002, 5001, peerIp);
                });
            }

            @Override
            public void onCallRejected(String calleeId, com.messenger.common.CallType type) {
                Platform.runLater(() -> {
                    chatArea.appendText(calleeId + " rejected your " + type + " call.\n");
                });
            }

            @Override
            public void onCallEnded(String calleeId) {
                Platform.runLater(() -> {
                    chatArea.appendText("Call ended by " + calleeId + "\n");
                    stopMediaResources();
                });
            }
        });
    }

    private void stopMediaResources() {
        if (mediaCapture != null)
            mediaCapture.stopCapture();
        if (audioReceiver != null)
            audioReceiver.stop();
        if (videoReceiver != null)
            videoReceiver.stop();
        if (audioSender != null)
            audioSender.stop();
        if (videoSender != null)
            videoSender.stop();
        if (audioOutputLine != null) {
            audioOutputLine.stop();
            audioOutputLine.close();
            audioOutputLine = null;
        }
        Platform.runLater(() -> {
            if (mediaStage != null && mediaStage.isShowing()) {
                mediaStage.close();
                mediaStage = null;
            }
        });
    }

    private void startMediaPresentation(com.messenger.common.CallType type, int listenPort, int sendPort,
            String peerIp) {
        audioReceiver = new com.messenger.client.media.StreamReceiver(listenPort);
        audioSender = new com.messenger.client.media.StreamSender(peerIp, sendPort);

        try {
            javax.sound.sampled.AudioFormat format = new javax.sound.sampled.AudioFormat(8000.0f, 16, 1, true, true);
            javax.sound.sampled.DataLine.Info info = new javax.sound.sampled.DataLine.Info(
                    javax.sound.sampled.SourceDataLine.class, format);
            audioOutputLine = (javax.sound.sampled.SourceDataLine) javax.sound.sampled.AudioSystem.getLine(info);
            audioOutputLine.open(format);
            audioOutputLine.start();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        if (type == com.messenger.common.CallType.AUDIO) {
            ProgressBar localAudioBar = new ProgressBar(0);
            ProgressBar remoteAudioBar = new ProgressBar(0);
            localAudioBar.setPrefWidth(200);
            remoteAudioBar.setPrefWidth(200);
            localAudioBar.setStyle("-fx-accent: #4fc3f7;");
            remoteAudioBar.setStyle("-fx-accent: #ff6b6b;");

            VBox audioLayout = new VBox(20,
                    new Label("Local Audio (You):"), localAudioBar,
                    new Label("Remote Audio (" + (selectedUser != null ? selectedUser.getUsername() : "Peer") + "):"),
                    remoteAudioBar);
            audioLayout.setPadding(new javafx.geometry.Insets(20));
            audioLayout.setAlignment(javafx.geometry.Pos.CENTER);

            Platform.runLater(() -> {
                mediaStage = new javafx.stage.Stage();
                mediaStage.setTitle("Audio Call Visualizer");
                mediaStage.setScene(new javafx.scene.Scene(audioLayout, 300, 200));
                mediaStage.setOnCloseRequest(ev -> {
                    if (selectedUser != null)
                        chatManager.endCall(selectedUser);
                    stopMediaResources();
                });
                mediaStage.show();
            });

            audioReceiver.startListening(data -> {
                if (audioOutputLine != null)
                    audioOutputLine.write(data, 0, data.length);
            }, amp -> Platform.runLater(() -> remoteAudioBar.setProgress(amp)));

            mediaCapture = new com.messenger.client.media.MediaCapture(audioSender, null);
            mediaCapture.startCapture(null, amp -> Platform.runLater(() -> localAudioBar.setProgress(amp)));

        } else if (type == com.messenger.common.CallType.VIDEO) {
            int videoListenPort = listenPort + 2;
            int videoSendPort = sendPort + 2;

            videoReceiver = new com.messenger.client.media.StreamReceiver(videoListenPort);
            videoSender = new com.messenger.client.media.StreamSender(peerIp, videoSendPort);

            javafx.scene.image.ImageView remoteVideo = new javafx.scene.image.ImageView();
            remoteVideo.setFitWidth(640);
            remoteVideo.setFitHeight(480);
            remoteVideo.setPreserveRatio(true);

            javafx.scene.image.ImageView localVideo = new javafx.scene.image.ImageView();
            localVideo.setFitWidth(160);
            localVideo.setFitHeight(120);
            localVideo.setPreserveRatio(true);

            // Add a subtle border to the local PIP to distinguish it from the remote
            // background
            localVideo.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 10, 0, 0, 0);");

            javafx.scene.layout.StackPane pipLayout = new javafx.scene.layout.StackPane(remoteVideo, localVideo);
            pipLayout.setStyle("-fx-background-color: #000;");
            javafx.scene.layout.StackPane.setAlignment(localVideo, javafx.geometry.Pos.BOTTOM_RIGHT);
            javafx.scene.layout.StackPane.setMargin(localVideo, new javafx.geometry.Insets(15));

            Platform.runLater(() -> {
                mediaStage = new javafx.stage.Stage();
                mediaStage.setTitle("Hybrid Video Call");
                mediaStage.setScene(new javafx.scene.Scene(pipLayout, 640, 480));
                mediaStage.setOnCloseRequest(ev -> {
                    if (selectedUser != null)
                        chatManager.endCall(selectedUser);
                    stopMediaResources();
                });
                mediaStage.show();
            });

            audioReceiver.startListening(data -> {
                if (audioOutputLine != null)
                    audioOutputLine.write(data, 0, data.length);
            });

            videoReceiver.startListening(data -> {
                try {
                    java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(data);
                    javafx.scene.image.Image img = new javafx.scene.image.Image(bais);
                    Platform.runLater(() -> remoteVideo.setImage(img));
                } catch (Exception ex) {
                }
            });

            mediaCapture = new com.messenger.client.media.MediaCapture(audioSender, videoSender);
            mediaCapture.startCapture(data -> {
                try {
                    java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(data);
                    javafx.scene.image.Image img = new javafx.scene.image.Image(bais);
                    Platform.runLater(() -> localVideo.setImage(img));
                } catch (Exception ex) {
                }
            }, null);
        }
    }

    public BorderPane getView() {
        return view;
    }
}
