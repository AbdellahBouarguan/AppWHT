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
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class ChatView {
    private BorderPane view;
    private ChatManager chatManager;
    private ListView<User> usersList;
    private VBox chatArea;
    private ScrollPane chatScroll;
    private User selectedUser;

    private com.messenger.client.media.MediaCapture mediaCapture;
    private com.messenger.client.media.StreamReceiver audioReceiver;
    private com.messenger.client.media.StreamReceiver videoReceiver;
    private com.messenger.client.media.StreamSender audioSender;
    private com.messenger.client.media.StreamSender videoSender;
    private javax.sound.sampled.SourceDataLine audioOutputLine;
    private javafx.stage.Stage mediaStage;

    private javafx.scene.layout.StackPane rootPane;

    public ChatView(ClientMain mainApp, ChatManager chatManager) {
        this.chatManager = chatManager;
        view = new BorderPane();
        view.setStyle("-fx-background-color: #111b21;");
        rootPane = new javafx.scene.layout.StackPane(view);

        // LEFT SIDEBAR
        VBox leftPane = new VBox();
        leftPane.setPrefWidth(350);
        leftPane.setMinWidth(300);
        leftPane.setStyle("-fx-border-color: #2f3b43; -fx-border-width: 0 1 0 0; -fx-background-color: #111b21;");

        // Sidebar Header
        HBox sidebarHeader = new HBox();
        sidebarHeader.setPrefHeight(59);
        sidebarHeader.setMinHeight(59);
        sidebarHeader.setStyle("-fx-background-color: #202c33;");
        sidebarHeader.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        sidebarHeader.setPadding(new javafx.geometry.Insets(0, 16, 0, 16));
        
        javafx.scene.shape.Circle myAvatar = new javafx.scene.shape.Circle(20, javafx.scene.paint.Color.web("#6b7c85"));
        Label myNameLabel = new Label(" Messenger");
        myNameLabel.setTextFill(javafx.scene.paint.Color.web("#e9edef"));
        myNameLabel.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 16));
        sidebarHeader.getChildren().addAll(myAvatar, myNameLabel);

        // Sidebar Search / Add Contact
        HBox searchContainer = new HBox(10);
        searchContainer.setPadding(new javafx.geometry.Insets(8, 12, 8, 12));
        searchContainer.setStyle("-fx-background-color: #111b21;");
        
        TextField addContactField = new TextField();
        addContactField.setPromptText("Add phone number");
        addContactField.setStyle("-fx-background-color: #202c33; -fx-text-fill: #e9edef; -fx-prompt-text-fill: #8696a0; -fx-background-radius: 8; -fx-padding: 8;");
        HBox.setHgrow(addContactField, Priority.ALWAYS);
        
        Button addContactBtn = new Button("+");
        addContactBtn.setStyle("-fx-background-color: #00a884; -fx-text-fill: #111b21; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;");
        addContactBtn.setOnAction(e -> {
            if (!addContactField.getText().trim().isEmpty()) {
                chatManager.addContact(addContactField.getText().trim());
                addContactField.clear();
            }
        });
        searchContainer.getChildren().addAll(addContactField, addContactBtn);

        // Chat List
        usersList = new ListView<>();
        usersList.setStyle("-fx-background-color: #111b21; -fx-control-inner-background: #111b21;");
        // Remove borders and focus rings from listview
        usersList.getStylesheets().add("data:text/css,.list-cell:filled:selected:focused,.list-cell:filled:selected{-fx-background-color: #2a3942;-fx-text-fill:white;} .list-cell{-fx-background-color:transparent;-fx-text-fill:#e9edef;-fx-font-size:16px;-fx-padding:12px;}");
        VBox.setVgrow(usersList, Priority.ALWAYS);
        usersList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            selectedUser = newV;
            if (selectedUser != null) {
                chatArea.getChildren().clear();
                chatManager.fetchChatHistory(selectedUser);
            }
        });

        leftPane.getChildren().addAll(sidebarHeader, searchContainer, usersList);
        view.setLeft(leftPane);

        // MAIN CHAT AREA
        VBox centerPane = new VBox();
        centerPane.setStyle("-fx-background-color: #0b141a;");

        // Chat Header
        HBox chatHeader = new HBox(15);
        chatHeader.setPrefHeight(59);
        chatHeader.setMinHeight(59);
        chatHeader.setStyle("-fx-background-color: #202c33;");
        chatHeader.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        chatHeader.setPadding(new javafx.geometry.Insets(0, 16, 0, 16));
        
        javafx.scene.shape.Circle contactAvatar = new javafx.scene.shape.Circle(20, javafx.scene.paint.Color.web("#6b7c85"));
        Label contactName = new Label("Select a contact");
        contactName.setTextFill(javafx.scene.paint.Color.web("#e9edef"));
        contactName.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 16));
        
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        javafx.scene.shape.SVGPath audioIcon = new javafx.scene.shape.SVGPath();
        audioIcon.setContent("M18.48,15.76C18.48,15.76,16.59,14.65,16.14,14.45C15.68,14.24,15.25,14.36,14.97,14.73C14.68,15.11,13.88,16.09,13.6,16.42C13.31,16.74,13.01,16.78,12.56,16.55C12.1,16.32,10.65,15.22,9.33,13.78C8.28,12.63,7.57,11.13,7.29,10.67C7.01,10.21,7.26,9.96,7.49,9.73C7.69,9.53,7.94,9.22,8.17,8.96C8.4,8.7,8.48,8.51,8.63,8.21C8.78,7.91,8.71,7.63,8.59,7.4C8.48,7.17,7.78,5.46,7.48,4.72C7.2,4.01,6.91,4.1,6.7,4.09C6.51,4.08,6.21,4.08,5.9,4.08C5.6,4.08,5.11,4.19,4.69,4.64C4.28,5.09,3.14,6.15,3.14,8.32C3.14,10.5,4.74,12.58,4.96,12.89C5.19,13.19,8.08,17.65,12.51,19.56C13.56,20.02,14.39,20.29,15.03,20.5C16.08,20.83,17.03,20.78,17.78,20.67C18.61,20.55,20.35,19.64,20.72,18.63C21.1,17.62,21.1,16.74,20.98,16.55C20.87,16.36,20.57,16.24,20.11,16.01L18.48,15.76Z");
        audioIcon.setFill(javafx.scene.paint.Color.web("#aebac1"));
        Button audioCallBtn = new Button("", audioIcon);
        audioCallBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        audioCallBtn.setOnMouseEntered(e -> audioIcon.setFill(javafx.scene.paint.Color.web("#e9edef")));
        audioCallBtn.setOnMouseExited(e -> audioIcon.setFill(javafx.scene.paint.Color.web("#aebac1")));

        javafx.scene.shape.SVGPath videoIcon = new javafx.scene.shape.SVGPath();
        videoIcon.setContent("M20.26,7.8L16,10.22V8c0-1.1-0.9-2-2-2H4C2.9,6,2,6.9,2,8v8c0,1.1,0.9,2,2,2h10c1.1,0,2-0.9,2-2v-2.22l4.26,2.42C20.7,16.44,21,16.12,21,15.64V8.36C21,7.88,20.7,7.56,20.26,7.8z");
        videoIcon.setFill(javafx.scene.paint.Color.web("#aebac1"));
        Button videoCallBtn = new Button("", videoIcon);
        videoCallBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        videoCallBtn.setOnMouseEntered(e -> videoIcon.setFill(javafx.scene.paint.Color.web("#e9edef")));
        videoCallBtn.setOnMouseExited(e -> videoIcon.setFill(javafx.scene.paint.Color.web("#aebac1")));

        javafx.scene.shape.SVGPath endCallIcon = new javafx.scene.shape.SVGPath();
        endCallIcon.setContent("M12,2C6.48,2,2,6.48,2,12s4.48,10,10,10s10-4.48,10-10S17.52,2,12,2z M17,13H7v-2h10V13z"); 
        endCallIcon.setFill(javafx.scene.paint.Color.web("#f15c6d"));
        Button endCallBtn = new Button("", endCallIcon);
        endCallBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");

        chatHeader.getChildren().addAll(contactAvatar, contactName, headerSpacer, audioCallBtn, videoCallBtn, endCallBtn);

        // Messages Area (Using VBox and ScrollPane for bubbles)
        chatArea = new VBox(8);
        chatArea.setStyle("-fx-background-color: #0b141a;");
        chatArea.setPadding(new javafx.geometry.Insets(10, 0, 10, 0));

        chatScroll = new ScrollPane(chatArea);
        chatScroll.setFitToWidth(true);
        chatScroll.setStyle("-fx-background: #0b141a; -fx-border-color: #0b141a;");
        chatScroll.getStylesheets().add("data:text/css,.scroll-pane > .viewport { -fx-background-color: #0b141a; } .scroll-bar:vertical { -fx-background-color: #0b141a; } .scroll-bar:vertical .thumb { -fx-background-color: #374045; -fx-background-radius: 5; }");
        chatScroll.vvalueProperty().bind(chatArea.heightProperty());
        VBox.setVgrow(chatScroll, Priority.ALWAYS);

        // Input Area
        HBox inputBox = new HBox(15);
        inputBox.setPrefHeight(62);
        inputBox.setMinHeight(62);
        inputBox.setAlignment(javafx.geometry.Pos.CENTER);
        inputBox.setPadding(new javafx.geometry.Insets(10, 16, 10, 16));
        inputBox.setStyle("-fx-background-color: #202c33;");

        TextField inputField = new TextField();
        inputField.setPromptText("Type a message");
        inputField.setStyle("-fx-background-color: #2a3942; -fx-text-fill: #e9edef; -fx-prompt-text-fill: #8696a0; -fx-background-radius: 8; -fx-padding: 10 15; -fx-font-size: 15;");
        HBox.setHgrow(inputField, Priority.ALWAYS);

        Button sendBtn = new Button("➤");
        sendBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8696a0; -fx-font-size: 20; -fx-cursor: hand;");

        inputBox.getChildren().addAll(inputField, sendBtn);

        centerPane.getChildren().addAll(chatHeader, chatScroll, inputBox);
        view.setCenter(centerPane);

        // Update contact name when user selected
        usersList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                contactName.setText(newV.getUsername());
            }
        });

        // Events
        sendBtn.setOnAction(e -> {
            if (selectedUser != null && !inputField.getText().isEmpty()) {
                com.messenger.common.Message msg = chatManager.sendMessage(selectedUser, inputField.getText());
                addMessageBubble(msg, true);
                inputField.clear();
            }
        });
        inputField.setOnAction(e -> sendBtn.fire());

        audioCallBtn.setOnAction(e -> {
            if (selectedUser != null) {
                if (!selectedUser.isOnline()) {
                    addSystemMessage("Cannot call offline user: " + selectedUser.getUsername());
                    return;
                }
                chatManager.requestCall(selectedUser, com.messenger.common.CallType.AUDIO);
                addSystemMessage("Initiating audio call to " + selectedUser.getUsername() + "...");
            }
        });

        videoCallBtn.setOnAction(e -> {
            if (selectedUser != null) {
                if (!selectedUser.isOnline()) {
                    addSystemMessage("Cannot call offline user: " + selectedUser.getUsername());
                    return;
                }
                chatManager.requestCall(selectedUser, com.messenger.common.CallType.VIDEO);
                addSystemMessage("Initiating video call to " + selectedUser.getUsername() + "...");
            }
        });

        endCallBtn.setOnAction(e -> {
            if (selectedUser != null) {
                chatManager.endCall(selectedUser);
                stopMediaResources();
                addSystemMessage("Ended call with " + selectedUser.getUsername());
            }
        });

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
            boolean isMe = msg.getSender().getId().equals(chatManager.getCurrentUser().getId());
            addMessageBubble(msg, isMe);
        });

        chatManager.setMessageAckListener(msg -> {
            Platform.runLater(() -> {
                for (javafx.scene.Node node : chatArea.getChildren()) {
                    if (("msg_" + msg.getId()).equals(node.getId())) {
                        if (node instanceof HBox) {
                            VBox bubbleContent = (VBox) ((HBox)node).getChildren().get(0);
                            Label timeLabel = (Label) bubbleContent.getChildren().get(1);
                            
                            java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
                            String timeStr = dtf.format(msg.getTimestamp());
                            String tickStr = "";
                            switch(msg.getStatus()) {
                                case SENDING: tickStr = " 🕒"; break;
                                case SENT_TO_SERVER: tickStr = " ✓"; break;
                                case DELIVERED_TO_CLIENT: tickStr = " ✓✓"; break;
                                case READ: tickStr = " ✓✓"; break;
                            }
                            timeLabel.setText(timeStr + tickStr);
                            timeLabel.setTextFill(msg.getStatus() == com.messenger.common.Message.MessageStatus.READ  
                                                  ? javafx.scene.paint.Color.web("#53bdeb") 
                                                  : javafx.scene.paint.Color.web("#8696a0"));
                        }
                        break;
                    }
                }
            });
        });

        chatManager.setChatHistoryListener(history -> {
            Platform.runLater(() -> {
                chatArea.getChildren().clear();
                for (com.messenger.common.Message m : history) {
                    boolean isMe = m.getSender().getId().equals(chatManager.getCurrentUser().getId());
                    addMessageBubble(m, isMe);
                }
            });
        });

        chatManager.setCallListener(new ChatManager.CallListener() {
            @Override
            public void onIncomingCall(com.messenger.common.User caller, com.messenger.common.CallType type) {
                Platform.runLater(() -> {
                    addCallEventBubble("Incoming " + type + " call from " + caller.getUsername() + "...", false);
                    
                    VBox callPopup = new VBox(15);
                    callPopup.setStyle("-fx-background-color: #202c33; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 10, 0, 0, 5);");
                    callPopup.setPadding(new javafx.geometry.Insets(25));
                    callPopup.setAlignment(javafx.geometry.Pos.CENTER);
                    callPopup.setMaxSize(320, 200);

                    Label titleLabel = new Label("Incoming " + type + " Call");
                    titleLabel.setStyle("-fx-text-fill: #e9edef; -fx-font-size: 18; -fx-font-weight: bold;");

                    Label nameLabel = new Label(caller.getUsername());
                    nameLabel.setStyle("-fx-text-fill: #8696a0; -fx-font-size: 14;");

                    HBox btnBox = new HBox(20);
                    btnBox.setAlignment(javafx.geometry.Pos.CENTER);

                    Button acceptBtn = new Button("Accept");
                    acceptBtn.setStyle("-fx-background-color: #00a884; -fx-text-fill: #111b21; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 24 8 24; -fx-cursor: hand;");
                    
                    Button rejectBtn = new Button("Decline");
                    rejectBtn.setStyle("-fx-background-color: #f15c6d; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 24 8 24; -fx-cursor: hand;");

                    btnBox.getChildren().addAll(rejectBtn, acceptBtn);
                    callPopup.getChildren().addAll(titleLabel, nameLabel, btnBox);
                    
                    rootPane.getChildren().add(callPopup);

                    acceptBtn.setOnAction(ev -> {
                        rootPane.getChildren().remove(callPopup);
                        chatManager.acceptCall(caller.getId(), type);
                        addCallEventBubble("Call accepted. Media starting...", false);

                            String peerIp = "127.0.0.1";
                            if (chatManager.getLatestCallData() != null && chatManager.getLatestCallData().length > 2) {
                                peerIp = (String) chatManager.getLatestCallData()[2];
                            }

                            startMediaPresentation(type, 5001, 5002, peerIp);
                    });
                    
                    rejectBtn.setOnAction(ev -> {
                        rootPane.getChildren().remove(callPopup);
                        chatManager.rejectCall(caller.getId(), type);
                        addCallEventBubble("Missed call from " + caller.getUsername(), true);
                    });
                });
            }

            @Override
            public void onCallAccepted(com.messenger.common.User callee, com.messenger.common.CallType type) {
                Platform.runLater(() -> {
                    addCallEventBubble(callee.getUsername() + " accepted your " + type + " call.", false);

                    String peerIp = "127.0.0.1";
                    if (chatManager.getLatestCallData() != null && chatManager.getLatestCallData().length > 2) {
                        peerIp = (String) chatManager.getLatestCallData()[2];
                    }

                    startMediaPresentation(type, 5002, 5001, peerIp);
                });
            }

            @Override
            public void onCallRejected(com.messenger.common.User callee, com.messenger.common.CallType type) {
                Platform.runLater(() -> {
                    addCallEventBubble(callee.getUsername() + " rejected your " + type + " call.", true);
                });
            }

            @Override
            public void onCallEnded(com.messenger.common.User callee) {
                Platform.runLater(() -> {
                    addCallEventBubble("Call ended by " + callee.getUsername(), false);
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

    public javafx.scene.layout.Pane getView() {
        return rootPane;
    }

    private void addMessageBubble(com.messenger.common.Message msg, boolean isMe) {
        Platform.runLater(() -> {
            Label msgLabel = new Label(msg.getContent());
            msgLabel.setWrapText(true);
            msgLabel.setFont(javafx.scene.text.Font.font("System", 15));
            msgLabel.setTextFill(javafx.scene.paint.Color.web("#e9edef"));
            
            java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
            String timeStr = dtf.format(msg.getTimestamp());
            String tickStr = "";
            if (isMe) {
                switch(msg.getStatus()) {
                    case SENDING: tickStr = " 🕒"; break;
                    case SENT_TO_SERVER: tickStr = " ✓"; break;
                    case DELIVERED_TO_CLIENT: tickStr = " ✓✓"; break;
                    case READ: tickStr = " ✓✓"; break;
                }
            }
            
            Label timeLabel = new Label(timeStr + tickStr);
            timeLabel.setFont(javafx.scene.text.Font.font("System", 11));
            timeLabel.setTextFill(msg.getStatus() == com.messenger.common.Message.MessageStatus.READ && isMe 
                                  ? javafx.scene.paint.Color.web("#53bdeb") 
                                  : javafx.scene.paint.Color.web("#8696a0"));
            
            VBox bubbleContent = new VBox(2, msgLabel, timeLabel);
            bubbleContent.setAlignment(isMe ? javafx.geometry.Pos.CENTER_RIGHT : javafx.geometry.Pos.BOTTOM_RIGHT);
            bubbleContent.setPadding(new javafx.geometry.Insets(6, 10, 6, 10));
            bubbleContent.setMaxWidth(400); // Max bubble width
            
            HBox hbox = new HBox();
            if (isMe) {
                bubbleContent.setStyle("-fx-background-color: #005c4b; -fx-background-radius: 8 0 8 8;");
                hbox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            } else {
                bubbleContent.setStyle("-fx-background-color: #202c33; -fx-background-radius: 0 8 8 8;");
                hbox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            }
            hbox.getChildren().add(bubbleContent);
            hbox.setPadding(new javafx.geometry.Insets(2, 20, 2, 20));
            hbox.setId("msg_" + msg.getId());
            chatArea.getChildren().add(hbox);
        });
    }

    private void addSystemMessage(String text) {
        Platform.runLater(() -> {
            Label sysLabel = new Label(text);
            sysLabel.setFont(javafx.scene.text.Font.font("System", 12));
            sysLabel.setTextFill(javafx.scene.paint.Color.web("#8696a0"));
            sysLabel.setPadding(new javafx.geometry.Insets(5, 10, 5, 10));
            sysLabel.setStyle("-fx-background-color: #182229; -fx-background-radius: 8;");
            
            HBox hbox = new HBox(sysLabel);
            hbox.setAlignment(javafx.geometry.Pos.CENTER);
            hbox.setPadding(new javafx.geometry.Insets(5, 0, 5, 0));
            chatArea.getChildren().add(hbox);
        });
    }

    private void addCallEventBubble(String text, boolean missed) {
        Platform.runLater(() -> {
            Label sysLabel = new Label(text);
            sysLabel.setFont(javafx.scene.text.Font.font("System", 13));
            sysLabel.setTextFill(javafx.scene.paint.Color.web("#e9edef"));
            
            javafx.scene.shape.SVGPath phoneIcon = new javafx.scene.shape.SVGPath();
            phoneIcon.setContent("M18.48,15.76C18.48,15.76,16.59,14.65,16.14,14.45C15.68,14.24,15.25,14.36,14.97,14.73C14.68,15.11,13.88,16.09,13.6,16.42C13.31,16.74,13.01,16.78,12.56,16.55C12.1,16.32,10.65,15.22,9.33,13.78C8.28,12.63,7.57,11.13,7.29,10.67C7.01,10.21,7.26,9.96,7.49,9.73C7.69,9.53,7.94,9.22,8.17,8.96C8.4,8.7,8.48,8.51,8.63,8.21C8.78,7.91,8.71,7.63,8.59,7.4C8.48,7.17,7.78,5.46,7.48,4.72C7.2,4.01,6.91,4.1,6.7,4.09C6.51,4.08,6.21,4.08,5.9,4.08C5.6,4.08,5.11,4.19,4.69,4.64C4.28,5.09,3.14,6.15,3.14,8.32C3.14,10.5,4.74,12.58,4.96,12.89C5.19,13.19,8.08,17.65,12.51,19.56C13.56,20.02,14.39,20.29,15.03,20.5C16.08,20.83,17.03,20.78,17.78,20.67C18.61,20.55,20.35,19.64,20.72,18.63C21.1,17.62,21.1,16.74,20.98,16.55C20.87,16.36,20.57,16.24,20.11,16.01L18.48,15.76Z");
            phoneIcon.setFill(missed ? javafx.scene.paint.Color.web("#f15c6d") : javafx.scene.paint.Color.web("#00a884"));
            phoneIcon.setScaleX(0.7);
            phoneIcon.setScaleY(0.7);

            HBox contentBox = new HBox(8, phoneIcon, sysLabel);
            contentBox.setAlignment(javafx.geometry.Pos.CENTER);
            contentBox.setPadding(new javafx.geometry.Insets(8, 14, 8, 14));
            contentBox.setStyle("-fx-background-color: #182229; -fx-background-radius: 12;");
            
            HBox hbox = new HBox(contentBox);
            hbox.setAlignment(javafx.geometry.Pos.CENTER);
            hbox.setPadding(new javafx.geometry.Insets(5, 0, 5, 0));
            chatArea.getChildren().add(hbox);
        });
    }
}
