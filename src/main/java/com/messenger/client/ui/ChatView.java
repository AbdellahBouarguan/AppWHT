package com.messenger.client.ui;

import com.messenger.client.ChatManager;
import com.messenger.client.ClientMain;
import com.messenger.common.User;
import java.util.List;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class ChatView {
    private BorderPane view;
    private ChatManager chatManager;
    private ListView<Object> usersList;
    private VBox chatArea;
    private ScrollPane chatScroll;
    private User selectedUser;
    private com.messenger.common.Group selectedGroup;
    private java.util.List<User> localContacts = new java.util.ArrayList<>();
    private java.util.List<com.messenger.common.Group> localGroups = new java.util.ArrayList<>();

    private com.messenger.client.media.MediaCapture mediaCapture;
    private com.messenger.client.media.StreamReceiver audioReceiver;
    private com.messenger.client.media.StreamReceiver videoReceiver;
    private com.messenger.client.media.StreamSender audioSender;
    private com.messenger.client.media.StreamSender videoSender;
    private javax.sound.sampled.SourceDataLine audioOutputLine;
    private com.messenger.client.util.AudioRecorder voiceRecorder = new com.messenger.client.util.AudioRecorder();
    private javafx.stage.Stage mediaStage;

    private int callSeconds = 0;
    private javafx.animation.Timeline callTimer;
    private boolean isMuted = false;
    private boolean isCameraOff = false;

    private javafx.scene.layout.StackPane rootPane;
    private final java.util.Map<String, java.util.List<User>> activeGroupCallMembers = new java.util.concurrent.ConcurrentHashMap<>();
    private GroupCallView activeCallView;
    private Label contactStatus;
    private HBox typingIndicatorBubble;
    private VBox replyPreviewContainer;
    private VBox linkPreviewArea;
    private com.messenger.client.util.LinkPreviewService.LinkMetadata currentLinkMetadata;
    private com.messenger.common.Message replyingToMessage;
    private Button audioCallBtn;
    private Button videoCallBtn;
    private Button endCallBtn;
    private Button joinCallBtn;

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

        javafx.scene.layout.StackPane myAvatarStack = createAvatarWithSilhouette(20);
        myAvatarStack.setCursor(javafx.scene.Cursor.HAND);
        myAvatarStack.setOnMouseClicked(e -> {
            javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
            fc.setTitle("Select Profile Picture");
            fc.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
            );
            java.io.File file = fc.showOpenDialog(view.getScene().getWindow());
            if (file != null) {
                if (file.length() > 5 * 1024 * 1024) {
                    addSystemMessage("Image is too large. Limit is 5MB.");
                    return;
                }
                try {
                    byte[] fileData = java.nio.file.Files.readAllBytes(file.toPath());
                    applyAvatarImage(myAvatarStack, fileData);
                    chatManager.updateProfilePicture(file);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        
        if (chatManager.getCurrentUser() != null && chatManager.getCurrentUser().getAvatarData() != null) {
            applyAvatarImage(myAvatarStack, chatManager.getCurrentUser().getAvatarData());
        }

        Label myNameLabel = new Label(" Messenger");
        myNameLabel.setTextFill(javafx.scene.paint.Color.web("#e9edef"));
        myNameLabel.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 16));
        
        Region headerSpacerLeft = new Region();
        HBox.setHgrow(headerSpacerLeft, Priority.ALWAYS);
        
        Button newGroupBtn = new Button("New Group");
        newGroupBtn.setStyle("-fx-background-color: #00a884; -fx-text-fill: #111b21; -fx-font-weight: bold; -fx-background-radius: 12; -fx-cursor: hand; -fx-font-size: 11; -fx-padding: 5 10 5 10;");
        newGroupBtn.setOnAction(e -> openNewGroupModal());
        
        sidebarHeader.getChildren().addAll(myAvatarStack, myNameLabel, headerSpacerLeft, newGroupBtn);

        // Sidebar Search / Add Contact
        HBox searchContainer = new HBox(10);
        searchContainer.setPadding(new javafx.geometry.Insets(8, 12, 8, 12));
        searchContainer.setStyle("-fx-background-color: #111b21;");

        TextField addContactField = new TextField();
        addContactField.setPromptText("Add phone number");
        addContactField.setStyle(
                "-fx-background-color: #202c33; -fx-text-fill: #e9edef; -fx-prompt-text-fill: #8696a0; -fx-background-radius: 8; -fx-padding: 8;");
        HBox.setHgrow(addContactField, Priority.ALWAYS);

        Button addContactBtn = new Button("+");
        addContactBtn.setStyle(
                "-fx-background-color: #00a884; -fx-text-fill: #111b21; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;");
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
        usersList.getStylesheets().add(
                "data:text/css,.list-cell:filled:selected:focused,.list-cell:filled:selected{-fx-background-color: #2a3942;-fx-text-fill:white;} .list-cell{-fx-background-color:transparent;-fx-text-fill:#e9edef;-fx-font-size:16px;-fx-padding:8px;}");
        VBox.setVgrow(usersList, Priority.ALWAYS);

        usersList.setCellFactory(lv -> new javafx.scene.control.ListCell<Object>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox box = new HBox(12);
                    box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    
                    if (item instanceof User) {
                        User user = (User) item;
                        javafx.scene.layout.StackPane avatar = createAvatarWithSilhouette(24);
                        if (user.getAvatarData() != null) {
                            applyAvatarImage(avatar, user.getAvatarData());
                        }
                        VBox texts = new VBox(2);
                        Label nameLbl = new Label(user.getUsername());
                        nameLbl.setTextFill(javafx.scene.paint.Color.web("#e9edef"));
                        nameLbl.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 15));

                        String statusText = user.isOnline() ? "Online" : formatLastSeen(user.getLastSeen());
                        if ("PENDING".equals(user.getRelationshipStatus())) {
                            statusText = "New Message (Pending)";
                        }
                        Label subLbl = new Label(statusText);
                        subLbl.setTextFill("PENDING".equals(user.getRelationshipStatus()) ? javafx.scene.paint.Color.web("#f15c6d")
                                : (user.isOnline() ? javafx.scene.paint.Color.web("#00a884") : javafx.scene.paint.Color.web("#8696a0")));
                        subLbl.setFont(javafx.scene.text.Font.font("System", 13));
                        
                        if ("PENDING".equals(user.getRelationshipStatus())) {
                            nameLbl.setText(user.getUsername() + " (New)");
                            nameLbl.setTextFill(javafx.scene.paint.Color.web("#53bdeb"));
                        }
                        texts.getChildren().addAll(nameLbl, subLbl);
                        box.getChildren().addAll(avatar, texts);
                    } else if (item instanceof com.messenger.common.Group) {
                        com.messenger.common.Group group = (com.messenger.common.Group) item;
                        
                        javafx.scene.layout.StackPane groupAvatar = new javafx.scene.layout.StackPane();
                        javafx.scene.shape.Circle avatarCircle = new javafx.scene.shape.Circle(24,
                                javafx.scene.paint.Color.web("#005c4b"));
                        
                        Label letterLabel = new Label(group.getName().isEmpty() ? "?" : group.getName().substring(0, 1).toUpperCase());
                        letterLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16;");
                        
                        groupAvatar.getChildren().addAll(avatarCircle, letterLabel);
                        
                        if (group.getGroupAvatar() != null) {
                            try {
                                javafx.scene.image.Image img = new javafx.scene.image.Image(
                                        new java.io.ByteArrayInputStream(group.getGroupAvatar()));
                                if (!img.isError()) {
                                    avatarCircle.setFill(new javafx.scene.paint.ImagePattern(img));
                                    letterLabel.setVisible(false);
                                }
                            } catch (Exception e) {}
                        }
                        
                        VBox texts = new VBox(2);
                        Label nameLbl = new Label(group.getName());
                        nameLbl.setTextFill(javafx.scene.paint.Color.web("#e9edef"));
                        nameLbl.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 15));

                        int membersCount = group.getMembers() != null ? group.getMembers().size() : 0;
                        Label subLbl = new Label("Group • " + membersCount + " members");
                        subLbl.setTextFill(javafx.scene.paint.Color.web("#8696a0"));
                        subLbl.setFont(javafx.scene.text.Font.font("System", 13));
                        
                        texts.getChildren().addAll(nameLbl, subLbl);
                        box.getChildren().addAll(groupAvatar, texts);
                    }
                    setGraphic(box);
                }
            }
        });
        usersList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV instanceof User) {
                selectedUser = (User) newV;
                selectedGroup = null;
                chatArea.getChildren().clear();
                chatManager.fetchChatHistory(selectedUser);
            } else if (newV instanceof com.messenger.common.Group) {
                selectedGroup = (com.messenger.common.Group) newV;
                selectedUser = null;
                chatArea.getChildren().clear();
                chatManager.fetchChatHistory(new User(selectedGroup.getId(), ""));
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

        javafx.scene.layout.StackPane contactAvatar = createAvatarWithSilhouette(20);
        Label contactName = new Label("Select a contact");
        contactName.setTextFill(javafx.scene.paint.Color.web("#e9edef"));
        contactName.setFont(javafx.scene.text.Font.font("System", javafx.scene.text.FontWeight.BOLD, 16));

        contactStatus = new Label("");
        contactStatus.setTextFill(javafx.scene.paint.Color.web("#8696a0"));
        contactStatus.setFont(javafx.scene.text.Font.font("System", 13));

        VBox nameBox = new VBox(2, contactName, contactStatus);
        nameBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        javafx.scene.shape.SVGPath audioIcon = new javafx.scene.shape.SVGPath();
        audioIcon.setContent(
                "M18.48,15.76C18.48,15.76,16.59,14.65,16.14,14.45C15.68,14.24,15.25,14.36,14.97,14.73C14.68,15.11,13.88,16.09,13.6,16.42C13.31,16.74,13.01,16.78,12.56,16.55C12.1,16.32,10.65,15.22,9.33,13.78C8.28,12.63,7.57,11.13,7.29,10.67C7.01,10.21,7.26,9.96,7.49,9.73C7.69,9.53,7.94,9.22,8.17,8.96C8.4,8.7,8.48,8.51,8.63,8.21C8.78,7.91,8.71,7.63,8.59,7.4C8.48,7.17,7.78,5.46,7.48,4.72C7.2,4.01,6.91,4.1,6.7,4.09C6.51,4.08,6.21,4.08,5.9,4.08C5.6,4.08,5.11,4.19,4.69,4.64C4.28,5.09,3.14,6.15,3.14,8.32C3.14,10.5,4.74,12.58,4.96,12.89C5.19,13.19,8.08,17.65,12.51,19.56C13.56,20.02,14.39,20.29,15.03,20.5C16.08,20.83,17.03,20.78,17.78,20.67C18.61,20.55,20.35,19.64,20.72,18.63C21.1,17.62,21.1,16.74,20.98,16.55C20.87,16.36,20.57,16.24,20.11,16.01L18.48,15.76Z");
        audioIcon.setFill(javafx.scene.paint.Color.web("#aebac1"));
        audioCallBtn = new Button("", audioIcon);
        audioCallBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        audioCallBtn.setOnMouseEntered(e -> audioIcon.setFill(javafx.scene.paint.Color.web("#e9edef")));
        audioCallBtn.setOnMouseExited(e -> audioIcon.setFill(javafx.scene.paint.Color.web("#aebac1")));

        javafx.scene.shape.SVGPath videoIcon = new javafx.scene.shape.SVGPath();
        videoIcon.setContent(
                "M20.26,7.8L16,10.22V8c0-1.1-0.9-2-2-2H4C2.9,6,2,6.9,2,8v8c0,1.1,0.9,2,2,2h10c1.1,0,2-0.9,2-2v-2.22l4.26,2.42C20.7,16.44,21,16.12,21,15.64V8.36C21,7.88,20.7,7.56,20.26,7.8z");
        videoIcon.setFill(javafx.scene.paint.Color.web("#aebac1"));
        videoCallBtn = new Button("", videoIcon);
        videoCallBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        videoCallBtn.setOnMouseEntered(e -> videoIcon.setFill(javafx.scene.paint.Color.web("#e9edef")));
        videoCallBtn.setOnMouseExited(e -> videoIcon.setFill(javafx.scene.paint.Color.web("#aebac1")));

        javafx.scene.shape.SVGPath endCallIcon = new javafx.scene.shape.SVGPath();
        endCallIcon.setContent("M12,2C6.48,2,2,6.48,2,12s4.48,10,10,10s10-4.48,10-10S17.52,2,12,2z M17,13H7v-2h10V13z");
        endCallIcon.setFill(javafx.scene.paint.Color.web("#f15c6d"));
        endCallBtn = new Button("", endCallIcon);
        endCallBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");

        joinCallBtn = new Button("Join Call");
        joinCallBtn.setStyle("-fx-background-color: #25D366; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 14; -fx-cursor: hand; -fx-font-size: 13; -fx-padding: 6 14 6 14;");
        joinCallBtn.setVisible(false);
        joinCallBtn.setManaged(false);
        joinCallBtn.setOnAction(e -> {
            if (selectedGroup != null) {
                chatManager.joinGroupCall(selectedGroup.getId());
                addSystemMessage("Joining group call for " + selectedGroup.getName() + "...");
            }
        });

        chatHeader.getChildren().addAll(contactAvatar, nameBox, headerSpacer, audioCallBtn, videoCallBtn, endCallBtn, joinCallBtn);

        // Messages Area (Using VBox and ScrollPane for bubbles)
        chatArea = new VBox(8);
        chatArea.setStyle("-fx-background-color: #0b141a;");
        chatArea.setPadding(new javafx.geometry.Insets(10, 0, 10, 0));

        chatScroll = new ScrollPane(chatArea);
        chatScroll.setFitToWidth(true);
        chatScroll.setStyle("-fx-background: #0b141a; -fx-border-color: #0b141a;");
        chatScroll.getStylesheets().add(
                "data:text/css,.scroll-pane > .viewport { -fx-background-color: #0b141a; } .scroll-bar:vertical { -fx-background-color: #0b141a; } .scroll-bar:vertical .thumb { -fx-background-color: #374045; -fx-background-radius: 5; }");
        chatScroll.vvalueProperty().bind(chatArea.heightProperty());
        VBox.setVgrow(chatScroll, Priority.ALWAYS);

        // Input Area
        HBox inputBox = new HBox(15);
        inputBox.setPrefHeight(62);
        inputBox.setMinHeight(62);
        inputBox.setAlignment(javafx.geometry.Pos.CENTER);
        inputBox.setPadding(new javafx.geometry.Insets(10, 16, 10, 16));
        inputBox.setStyle("-fx-background-color: #202c33;");

        javafx.scene.shape.SVGPath attachIcon = new javafx.scene.shape.SVGPath();
        attachIcon.setContent(
                "M16.5,6v11.5c0,2.21-1.79,4-4,4s-4-1.79-4-4V5c0-1.38,1.12-2.5,2.5-2.5s2.5,1.12,2.5,2.5v10.5c0,0.55-0.45,1-1,1s-1-0.45-1-1V6H10v9.5c0,1.38,1.12,2.5,2.5,2.5s2.5-1.12,2.5-2.5V5c0-2.21-1.79-4-4-4S7,2.79,7,5v12.5c0,3.04,2.46,5.5,5.5,5.5s5.5-2.46,5.5-5.5V6H16.5z");
        attachIcon.setFill(javafx.scene.paint.Color.web("#8696a0"));
        Button attachBtn = new Button("", attachIcon);
        attachBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");

        TextField inputField = new TextField();
        inputField.setPromptText("Type a message");
        inputField.setStyle(
                "-fx-background-color: #2a3942; -fx-text-fill: #e9edef; -fx-prompt-text-fill: #8696a0; -fx-background-radius: 8; -fx-padding: 10 15; -fx-font-size: 15;");
        HBox.setHgrow(inputField, Priority.ALWAYS);

        javafx.animation.PauseTransition debounce = new javafx.animation.PauseTransition(
                javafx.util.Duration.millis(500));
        final AtomicReference<String> lastUrl = new AtomicReference<>(null);

        inputField.textProperty().addListener((obs, oldV, newV) -> {
            debounce.setOnFinished(event -> {
                String url = com.messenger.client.util.LinkPreviewService.extractUrl(newV);
                if (url != null) {
                    if (url.equals(lastUrl.get()))
                        return;
                    lastUrl.set(url);

                    new Thread(() -> {
                        com.messenger.client.util.LinkPreviewService.LinkMetadata meta = com.messenger.client.util.LinkPreviewService
                                .fetchMetadata(url);
                        Platform.runLater(() -> {
                            if (meta != null) {
                                currentLinkMetadata = meta;
                                showLinkPreview(meta);
                            }
                        });
                    }).start();
                } else {
                    lastUrl.set(null);
                    currentLinkMetadata = null;
                    Platform.runLater(() -> linkPreviewArea.getChildren().clear());
                }
            });
            debounce.playFromStart();
        });

        Button sendBtn = new Button("➤");
        sendBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #8696a0; -fx-font-size: 20; -fx-cursor: hand;");

        javafx.scene.shape.SVGPath micIcon = new javafx.scene.shape.SVGPath();
        micIcon.setContent(
                "M12,14c1.66,0,3-1.34,3-3V5c0-1.66-1.34-3-3-3S9,3.34,9,5v6C9,12.66,10.34,14,12,14z M17,11c0,2.76-2.24,5-5,5s-5-2.24-5-5H5c0,3.53,2.61,6.43,6,6.92V21h2v-3.08c3.39-0.49,6-3.39,6-6.92H17z");
        micIcon.setFill(javafx.scene.paint.Color.web("#8696a0"));
        Button micBtn = new Button("", micIcon);
        micBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");

        micBtn.setOnMousePressed(e -> {
            if (selectedUser != null || selectedGroup != null) {
                try {
                    voiceRecorder.start();
                    micIcon.setFill(javafx.scene.paint.Color.RED);
                    inputField.setPromptText("Recording...");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        micBtn.setOnMouseReleased(e -> {
            if (voiceRecorder.isRecording()) {
                byte[] audio = voiceRecorder.stop();
                micIcon.setFill(javafx.scene.paint.Color.web("#8696a0"));
                inputField.setPromptText("Type a message");
                if (audio != null && audio.length > 500) {
                    if (selectedGroup != null) {
                        chatManager.sendGroupMessage(selectedGroup.getId(), "[Voice Message]",
                                "voice_" + System.currentTimeMillis() + ".wav", audio, replyingToMessage);
                    } else {
                        chatManager.sendMessage(selectedUser.getId(), "[Voice Message]",
                                "voice_" + System.currentTimeMillis() + ".wav", audio, replyingToMessage);
                    }
                    cancelReply();
                }
            }
        });

        inputBox.getChildren().addAll(attachBtn, inputField, sendBtn, micBtn);

        replyPreviewContainer = new VBox();
        replyPreviewContainer.setStyle("-fx-background-color: #0b141a;");
        replyPreviewContainer.setPadding(new javafx.geometry.Insets(0, 16, 0, 16));

        linkPreviewArea = new VBox();
        linkPreviewArea.setStyle("-fx-background-color: #0b141a;");
        linkPreviewArea.setPadding(new javafx.geometry.Insets(0, 16, 5, 16));

        centerPane.getChildren().addAll(chatHeader, chatScroll, replyPreviewContainer, linkPreviewArea, inputBox);
        view.setCenter(centerPane);

        usersList.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV instanceof User) {
                User u = (User) newV;
                contactName.setText(u.getUsername());
                contactStatus.setText(u.isOnline() ? "online" : formatLastSeen(u.getLastSeen()));
                contactStatus.setTextFill(javafx.scene.paint.Color.web("#8696a0"));

                applyAvatarImage(contactAvatar, u.getAvatarData());
                showTypingIndicator(false);
                audioCallBtn.setVisible(true);
                audioCallBtn.setManaged(true);
                videoCallBtn.setVisible(true);
                videoCallBtn.setManaged(true);
                joinCallBtn.setVisible(false);
                joinCallBtn.setManaged(false);
                endCallBtn.setVisible(false);
                endCallBtn.setManaged(false);
            } else if (newV instanceof com.messenger.common.Group) {
                com.messenger.common.Group g = (com.messenger.common.Group) newV;
                contactName.setText(g.getName());
                
                StringBuilder sb = new StringBuilder();
                if (g.getMembers() != null) {
                    for (int i = 0; i < g.getMembers().size(); i++) {
                        sb.append(g.getMembers().get(i).getUsername());
                        if (i < g.getMembers().size() - 1) sb.append(", ");
                    }
                }
                contactStatus.setText(sb.toString());
                contactStatus.setTextFill(javafx.scene.paint.Color.web("#8696a0"));

                applyAvatarImage(contactAvatar, g.getGroupAvatar());
                showTypingIndicator(false);
                audioCallBtn.setVisible(true);
                audioCallBtn.setManaged(true);
                videoCallBtn.setVisible(true);
                videoCallBtn.setManaged(true);
                joinCallBtn.setVisible(false);
                joinCallBtn.setManaged(false);
                endCallBtn.setVisible(false);
                endCallBtn.setManaged(false);
                
                java.util.List<User> activeCallUsers = activeGroupCallMembers.get(g.getId());
                if (activeCallUsers != null && !activeCallUsers.isEmpty()) {
                    updateHeaderBarForGroup(g.getId(), activeCallUsers);
                } else {
                    updateHeaderBarForGroup(g.getId(), new java.util.ArrayList<>());
                }
            }
        });

        sendBtn.setOnAction(e -> {
            if ((selectedUser != null || selectedGroup != null) && !inputField.getText().isEmpty()) {
                com.messenger.common.Message msg;
                if (selectedGroup != null) {
                    msg = chatManager.sendGroupMessage(selectedGroup.getId(), inputField.getText(), replyingToMessage);
                } else {
                    msg = new com.messenger.common.Message(chatManager.getCurrentUser(),
                            selectedUser, inputField.getText());
                    if (replyingToMessage != null) {
                        msg.setParentMessageId(replyingToMessage.getId());
                        msg.setParentMessageContent(replyingToMessage.getContent());
                    }
                    if (currentLinkMetadata != null) {
                        msg.setLinkTitle(currentLinkMetadata.title);
                        msg.setLinkDescription(currentLinkMetadata.description);
                        msg.setLinkImageUrl(currentLinkMetadata.imageUrl);
                    }
                    chatManager.sendDirectMessage(msg);
                }
                addMessageBubble(msg, true);
                inputField.clear();
                cancelReply();
                currentLinkMetadata = null;
                linkPreviewArea.getChildren().clear();
            }
        });
        inputField.setOnAction(e -> sendBtn.fire());

        attachBtn.setOnAction(e -> {
            if (selectedUser == null && selectedGroup == null)
                return;
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Select File to Send");
            java.io.File file = fileChooser.showOpenDialog(view.getScene().getWindow());
            if (file != null) {
                if (file.length() > 5 * 1024 * 1024) {
                    addSystemMessage("File too large. Limit is 5MB.");
                    return;
                }
                com.messenger.common.Message msg;
                if (selectedGroup != null) {
                    try {
                        byte[] fileData = java.nio.file.Files.readAllBytes(file.toPath());
                        msg = chatManager.sendGroupMessage(selectedGroup.getId(), inputField.getText(), file.getName(), fileData, replyingToMessage);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        return;
                    }
                } else {
                    msg = chatManager.sendFileMessage(selectedUser, inputField.getText(), file, replyingToMessage);
                }
                addMessageBubble(msg, true);
                inputField.clear();
                cancelReply();
            }
        });

        AtomicBoolean isTypingSent = new AtomicBoolean(false);
        java.util.Timer[] typingTimer = new java.util.Timer[1];

        inputField.textProperty().addListener((obs, oldText, newText) -> {
            String targetId = selectedGroup != null ? selectedGroup.getId() : (selectedUser != null ? selectedUser.getId() : null);
            if (targetId == null)
                return;

            if (!newText.isEmpty()) {
                if (!isTypingSent.get()) {
                    chatManager.sendTypingUpdate(targetId, true);
                    isTypingSent.set(true);
                }

                if (typingTimer[0] != null) {
                    typingTimer[0].cancel();
                }
                typingTimer[0] = new java.util.Timer();
                typingTimer[0].schedule(new java.util.TimerTask() {
                    @Override
                    public void run() {
                        chatManager.sendTypingUpdate(targetId, false);
                        isTypingSent.set(false);
                    }
                }, 1500); // Stop typing after 1.5s
            } else {
                if (isTypingSent.get()) {
                    chatManager.sendTypingUpdate(targetId, false);
                    isTypingSent.set(false);
                    if (typingTimer[0] != null)
                        typingTimer[0].cancel();
                }
            }
        });

        audioCallBtn.setOnAction(e -> {
            if (selectedUser != null) {
                if (!selectedUser.isOnline()) {
                    addSystemMessage("Cannot call offline user: " + selectedUser.getUsername());
                    return;
                }
                chatManager.requestCall(selectedUser, com.messenger.common.CallType.AUDIO);
                addSystemMessage("Initiating audio call to " + selectedUser.getUsername() + "...");
            } else if (selectedGroup != null) {
                chatManager.joinGroupCall(selectedGroup.getId());
                addSystemMessage("Joining group call for " + selectedGroup.getName() + "...");
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
            } else if (selectedGroup != null) {
                chatManager.joinGroupCall(selectedGroup.getId());
                addSystemMessage("Joining group video call for " + selectedGroup.getName() + "...");
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
                localContacts.clear();
                localContacts.addAll(users);
                updateUnifiedSidebar();
            });
        });

        chatManager.setGroupListListener(groups -> {
            Platform.runLater(() -> {
                localGroups.clear();
                localGroups.addAll(groups);
                updateUnifiedSidebar();
            });
        });

        chatManager.setGroupCreatedListener(group -> {
            Platform.runLater(() -> {
                if (!localGroups.stream().anyMatch(g -> g.getId().equals(group.getId()))) {
                    localGroups.add(group);
                    updateUnifiedSidebar();
                }
            });
        });

        chatManager.setStatusUpdateListener(updatedUser -> {
            Platform.runLater(() -> {
                // Update in localContacts list
                for (User u : localContacts) {
                    if (u.getId().equals(updatedUser.getId())) {
                        u.setOnline(updatedUser.isOnline());
                        u.setLastSeen(updatedUser.getLastSeen());
                        break;
                    }
                }
                updateUnifiedSidebar();
                usersList.refresh();

                // Update Header if active chat
                if (selectedUser != null && selectedUser.getId().equals(updatedUser.getId())) {
                    selectedUser.setOnline(updatedUser.isOnline());
                    selectedUser.setLastSeen(updatedUser.getLastSeen());
                    if (!"typing...".equals(contactStatus.getText())) {
                        contactStatus
                                .setText(updatedUser.isOnline() ? "online" : formatLastSeen(updatedUser.getLastSeen()));
                        contactStatus.setTextFill(javafx.scene.paint.Color.web("#8696a0"));
                    }
                }
            });
        });

        chatManager.setTypingListener((senderId, targetId, isTyping) -> {
            Platform.runLater(() -> {
                if (selectedGroup != null && selectedGroup.getId().equals(targetId)) {
                    if (isTyping) {
                        String typerName = "Someone";
                        for (User u : selectedGroup.getMembers()) {
                            if (u.getId().equals(senderId)) {
                                typerName = u.getUsername();
                                break;
                            }
                        }
                        contactStatus.setText(typerName + " is typing...");
                        contactStatus.setTextFill(javafx.scene.paint.Color.web("#00a884"));
                        showTypingIndicator(true);
                    } else {
                        StringBuilder sb = new StringBuilder();
                        if (selectedGroup.getMembers() != null) {
                            for (int i = 0; i < selectedGroup.getMembers().size(); i++) {
                                sb.append(selectedGroup.getMembers().get(i).getUsername());
                                if (i < selectedGroup.getMembers().size() - 1) sb.append(", ");
                            }
                        }
                        contactStatus.setText(sb.toString());
                        contactStatus.setTextFill(javafx.scene.paint.Color.web("#8696a0"));
                        showTypingIndicator(false);
                    }
                } else if (selectedUser != null && selectedUser.getId().equals(senderId)) {
                    if (isTyping) {
                        contactStatus.setText("typing...");
                        contactStatus.setTextFill(javafx.scene.paint.Color.web("#00a884")); // WhatsApp green
                        showTypingIndicator(true);
                    } else {
                        contactStatus.setText(selectedUser.isOnline() ? "online" : formatLastSeen(selectedUser.getLastSeen()));
                        contactStatus.setTextFill(javafx.scene.paint.Color.web("#8696a0"));
                        showTypingIndicator(false);
                    }
                }
            });
        });

        chatManager.setMessageListener(msg -> {
            boolean isMe = msg.getSender().getId().equals(chatManager.getCurrentUser().getId());
            boolean isCurrentChat = false;
            if (selectedUser != null) {
                isCurrentChat = isMe ? (msg.getReceiver() != null && msg.getReceiver().getId().equals(selectedUser.getId())) : msg.getSender().getId().equals(selectedUser.getId());
            } else if (selectedGroup != null) {
                isCurrentChat = msg.getGroupId() != null && msg.getGroupId().equals(selectedGroup.getId());
            }
            
            if (isCurrentChat) {
                addMessageBubble(msg, isMe);
                if (!isMe && selectedUser != null) {
                    chatManager.sendReadReceipt(selectedUser.getId());
                }
            }
        });

        chatManager.setMessageAckListener(msg -> {
            Platform.runLater(() -> {
                for (javafx.scene.Node node : chatArea.getChildren()) {
                    if (("msg_" + msg.getId()).equals(node.getId())) {
                        if (node instanceof HBox) {
                            VBox bubbleContent = (VBox) ((HBox) node).getChildren().get(0);
                            Label timeLabel = (Label) bubbleContent.getChildren().get(1);

                            java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter
                                    .ofPattern("HH:mm");
                            String timeStr = dtf.format(msg.getTimestamp());
                            String tickStr = "";
                            switch (msg.getStatus()) {
                                case SENDING:
                                    tickStr = " 🕒";
                                    break;
                                case SENT_TO_SERVER:
                                    tickStr = " ✓";
                                    break;
                                case DELIVERED_TO_CLIENT:
                                    tickStr = " ✓✓";
                                    break;
                                case READ:
                                    tickStr = " ✓✓";
                                    break;
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

        chatManager.setMessagesReadListener(readerId -> {
            Platform.runLater(() -> {
                if (selectedUser != null && selectedUser.getId().equals(readerId)) {
                    for (javafx.scene.Node node : chatArea.getChildren()) {
                        if (node instanceof HBox && node.getId() != null && node.getId().startsWith("msg_")) {
                            VBox bubbleContent = (VBox) ((HBox) node).getChildren().get(0);
                            Label timeLabel = null;
                            for (javafx.scene.Node child : bubbleContent.getChildren()) {
                                if (child instanceof Label && ((Label) child).getText() != null
                                        && ((Label) child).getText().contains("✓")) {
                                    timeLabel = (Label) child;
                                    break;
                                }
                            }
                            if (timeLabel != null && timeLabel.getText().contains("✓✓")) {
                                timeLabel.setTextFill(javafx.scene.paint.Color.web("#53bdeb"));
                            }
                        }
                    }
                }
            });
        });

        chatManager.setMessageDeletedListener(msgUuid -> {
            updateMessageAsDeleted(msgUuid);
        });

        chatManager.setMessageReactionListener((msgUuid, userId, emoji) -> {
            updateMessageReaction(msgUuid, userId, emoji);
        });

        chatManager.setChatHistoryListener(history -> {
            Platform.runLater(() -> {
                chatArea.getChildren().clear();
                for (com.messenger.common.Message m : history) {
                    boolean isMe = m.getSender().getId().equals(chatManager.getCurrentUser().getId());
                    addMessageBubble(m, isMe);
                }
                
                checkAndShowContactBanner();

                if (selectedUser != null) {
                    chatManager.sendReadReceipt(selectedUser.getId());
                }
            });
        });

        chatManager.setCallListener(new ChatManager.CallListener() {
            @Override
            public void onIncomingCall(com.messenger.common.User caller, com.messenger.common.CallType type) {
                Platform.runLater(() -> {
                    addCallEventBubble("Incoming " + type + " call from " + caller.getUsername() + "...", false);

                    VBox callPopup = new VBox(15);
                    callPopup.setStyle(
                            "-fx-background-color: #202c33; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 10, 0, 0, 5);");
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
                    acceptBtn.setStyle(
                            "-fx-background-color: #00a884; -fx-text-fill: #111b21; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 24 8 24; -fx-cursor: hand;");

                    Button rejectBtn = new Button("Decline");
                    rejectBtn.setStyle(
                            "-fx-background-color: #f15c6d; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 24 8 24; -fx-cursor: hand;");

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

        chatManager.setGroupCallListener(new ChatManager.GroupCallListener() {
            @Override
            public void onGroupCallJoinSuccess(String gId, int audioPort, int videoPort, List<User> activeParticipants) {
                Platform.runLater(() -> {
                    if (activeCallView != null && activeCallView.getGroupId().equals(gId)) {
                        activeCallView.updateParticipants(activeParticipants);
                    } else {
                        String sIp = "127.0.0.1";
                        if (chatManager.getNetworkClient() != null) {
                            sIp = chatManager.getNetworkClient().getServerIp();
                        }
                        com.messenger.common.Group group = localGroups.stream()
                                .filter(g -> g.getId().equals(gId))
                                .findFirst()
                                .orElse(new com.messenger.common.Group(gId, "Group Chat", "", "", java.time.LocalDateTime.now()));
                                
                        activeCallView = new GroupCallView(gId, group.getName(), chatManager, sIp, audioPort, videoPort, activeParticipants);
                        activeCallView.setOnClose(() -> {
                            activeCallView = null;
                        });
                        activeCallView.show();
                    }
                });
            }

            @Override
            public void onGroupCallStateUpdated(String gId, List<User> activeParticipants) {
                Platform.runLater(() -> {
                    boolean isCallActive = activeParticipants != null && !activeParticipants.isEmpty();

                    if (isCallActive) {
                        activeGroupCallMembers.put(gId, activeParticipants);
                    } else {
                        activeGroupCallMembers.remove(gId);
                    }
                    
                    if (activeCallView != null && activeCallView.getGroupId().equals(gId)) {
                        activeCallView.updateParticipants(activeParticipants);
                    }
                    
                    updateHeaderBarForGroup(gId, activeParticipants);
                });
            }

            @Override
            public void onGroupCallStarted(String gId) {
                Platform.runLater(() -> {
                    com.messenger.common.Group group = localGroups.stream()
                            .filter(g -> g.getId().equals(gId))
                            .findFirst()
                            .orElse(null);
                    String gName = group != null ? group.getName() : "Group Chat";
                    showGroupCallNotification(gName, gId);
                });
            }
        });
    }

    private void updateHeaderBarForGroup(String gId, List<User> activeParticipants) {
        Platform.runLater(() -> {
            if (selectedGroup != null && selectedGroup.getId().equals(gId)) {
                if (activeParticipants.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    if (selectedGroup.getMembers() != null) {
                        for (int i = 0; i < selectedGroup.getMembers().size(); i++) {
                            sb.append(selectedGroup.getMembers().get(i).getUsername());
                            if (i < selectedGroup.getMembers().size() - 1) sb.append(", ");
                        }
                    }
                    contactStatus.setText(sb.toString());
                    contactStatus.setTextFill(javafx.scene.paint.Color.web("#8696a0"));

                    audioCallBtn.setVisible(true);
                    audioCallBtn.setManaged(true);
                    videoCallBtn.setVisible(true);
                    videoCallBtn.setManaged(true);
                    joinCallBtn.setVisible(false);
                    joinCallBtn.setManaged(false);
                } else {
                    StringBuilder sb = new StringBuilder("🟢 Active Call (");
                    for (int i = 0; i < activeParticipants.size(); i++) {
                        sb.append(activeParticipants.get(i).getUsername());
                        if (i < activeParticipants.size() - 1) sb.append(", ");
                    }
                    sb.append(")");
                    contactStatus.setText(sb.toString());
                    contactStatus.setTextFill(javafx.scene.paint.Color.web("#25D366"));

                    audioCallBtn.setVisible(false);
                    audioCallBtn.setManaged(false);
                    videoCallBtn.setVisible(false);
                    videoCallBtn.setManaged(false);
                    joinCallBtn.setVisible(true);
                    joinCallBtn.setManaged(true);
                }
            }
        });
    }

    private void showGroupCallNotification(String groupName, String groupId) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Group Call Started");
            alert.setHeaderText("Active Group Call");
            alert.setContentText("A call has started in group '" + groupName + "'. Do you want to join?");
            
            ButtonType joinButtonType = new ButtonType("Join Now");
            ButtonType dismissButtonType = new ButtonType("Dismiss", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(joinButtonType, dismissButtonType);
            
            alert.showAndWait().ifPresent(response -> {
                if (response == joinButtonType) {
                    for (Object item : usersList.getItems()) {
                        if (item instanceof com.messenger.common.Group) {
                            com.messenger.common.Group g = (com.messenger.common.Group) item;
                            if (g.getId().equals(groupId)) {
                                usersList.getSelectionModel().select(g);
                                chatManager.joinGroupCall(groupId);
                                addSystemMessage("Joining group call for " + g.getName() + "...");
                                break;
                            }
                        }
                    }
                }
            });
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
            if (callTimer != null) {
                callTimer.stop();
                callTimer = null;
            }
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

                Label timerLabel = new Label("00:00");
                timerLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18;");

                callSeconds = 0;
                callTimer = new javafx.animation.Timeline(
                        new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> {
                            callSeconds++;
                            int mins = callSeconds / 60;
                            int secs = callSeconds % 60;
                            timerLabel.setText(String.format("%02d:%02d", mins, secs));
                        }));
                callTimer.setCycleCount(javafx.animation.Animation.INDEFINITE);
                callTimer.play();

                Button muteBtn = new Button("🎤 Mute");
                muteBtn.setStyle("-fx-background-color: #374045; -fx-text-fill: white;");
                muteBtn.setOnAction(e -> {
                    isMuted = !isMuted;
                    muteBtn.setText(isMuted ? "🔇 Unmute" : "🎤 Mute");
                    if (mediaCapture != null)
                        mediaCapture.setAudioEnabled(!isMuted);
                });

                HBox controls = new HBox(10, timerLabel, muteBtn);
                controls.setAlignment(javafx.geometry.Pos.CENTER);

                VBox fullLayout = new VBox(15, audioLayout, controls);
                fullLayout.setStyle("-fx-background-color: #0b141a;");
                fullLayout.setAlignment(javafx.geometry.Pos.CENTER);

                mediaStage.setScene(new javafx.scene.Scene(fullLayout, 300, 250));
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

                Label timerLabel = new Label("00:00");
                timerLabel.setStyle(
                        "-fx-text-fill: white; -fx-font-size: 16; -fx-background-color: rgba(0,0,0,0.5); -fx-padding: 5;");

                callSeconds = 0;
                callTimer = new javafx.animation.Timeline(
                        new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> {
                            callSeconds++;
                            int mins = callSeconds / 60;
                            int secs = callSeconds % 60;
                            timerLabel.setText(String.format("%02d:%02d", mins, secs));
                        }));
                callTimer.setCycleCount(javafx.animation.Animation.INDEFINITE);
                callTimer.play();

                Button muteBtn = new Button("Mute");
                Button camBtn = new Button("Cam Off");
                muteBtn.setStyle("-fx-background-color: #374045; -fx-text-fill: white;");
                camBtn.setStyle("-fx-background-color: #374045; -fx-text-fill: white;");

                muteBtn.setOnAction(e -> {
                    isMuted = !isMuted;
                    muteBtn.setText(isMuted ? "Unmute" : "Mute");
                    if (mediaCapture != null)
                        mediaCapture.setAudioEnabled(!isMuted);
                });

                camBtn.setOnAction(e -> {
                    isCameraOff = !isCameraOff;
                    camBtn.setText(isCameraOff ? "Cam On" : "Cam Off");
                    if (mediaCapture != null)
                        mediaCapture.setVideoEnabled(!isCameraOff);
                });

                HBox controls = new HBox(15, muteBtn, camBtn);
                controls.setAlignment(javafx.geometry.Pos.CENTER);
                controls.setStyle("-fx-background-color: rgba(0,0,0,0.6); -fx-padding: 10;");

                javafx.scene.layout.StackPane finalLayout = new javafx.scene.layout.StackPane(pipLayout);
                VBox overlay = new VBox(10, timerLabel);
                overlay.setAlignment(javafx.geometry.Pos.TOP_LEFT);
                overlay.setPadding(new javafx.geometry.Insets(10));

                VBox bottomOverlay = new VBox(controls);
                bottomOverlay.setAlignment(javafx.geometry.Pos.BOTTOM_CENTER);

                finalLayout.getChildren().addAll(overlay, bottomOverlay);

                mediaStage.setScene(new javafx.scene.Scene(finalLayout, 640, 480));
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

    private void startReply(com.messenger.common.Message msg) {
        replyingToMessage = msg;
        replyPreviewContainer.getChildren().clear();

        HBox previewBox = new HBox(10);
        previewBox.setPadding(new javafx.geometry.Insets(10));
        previewBox.setStyle(
                "-fx-background-color: #202c33; -fx-border-color: #00a884; -fx-border-width: 0 0 0 4; -fx-border-radius: 4; -fx-background-radius: 4;");

        VBox contentBox = new VBox(5);
        Label senderLabel = new Label(msg.getSender().getUsername());
        senderLabel.setStyle("-fx-text-fill: #00a884; -fx-font-weight: bold;");
        Label textLabel = new Label(msg.getContent());
        textLabel.setStyle("-fx-text-fill: #8696a0;");
        textLabel.setWrapText(true);
        textLabel.setMaxWidth(300);
        contentBox.getChildren().addAll(senderLabel, textLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8696a0; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> cancelReply());

        previewBox.getChildren().addAll(contentBox, spacer, closeBtn);
        replyPreviewContainer.getChildren().add(previewBox);
    }

    private void cancelReply() {
        replyingToMessage = null;
        replyPreviewContainer.getChildren().clear();
    }

    private void updateMessageAsDeleted(String uuid) {
        Platform.runLater(() -> {
            for (javafx.scene.Node node : chatArea.getChildren()) {
                if (("msg_" + uuid).equals(node.getId())) {
                    if (node instanceof HBox) {
                        VBox bubbleContent = (VBox) ((HBox) node).getChildren().get(0);
                        VBox mainContent = (VBox) bubbleContent.getChildren().get(0);
                        mainContent.getChildren().clear();
                        Label deletedLabel = new Label("This message was deleted");
                        deletedLabel.setStyle("-fx-text-fill: #8696a0; -fx-font-style: italic; -fx-font-size: 14;");
                        mainContent.getChildren().add(deletedLabel);
                    }
                    break;
                }
            }
        });
    }

    private void updateMessageReaction(String uuid, String userId, String emoji) {
        Platform.runLater(() -> {
            for (javafx.scene.Node node : chatArea.getChildren()) {
                if (("msg_" + uuid).equals(node.getId())) {
                    if (node instanceof HBox) {
                        VBox bubbleContent = (VBox) ((HBox) node).getChildren().get(0);
                        HBox reactionsBar = null;
                        for (javafx.scene.Node child : bubbleContent.getChildren()) {
                            if (child.getId() != null && child.getId().equals("reactions_" + uuid)) {
                                reactionsBar = (HBox) child;
                                break;
                            }
                        }
                        if (reactionsBar == null) {
                            reactionsBar = new HBox(3);
                            reactionsBar.setId("reactions_" + uuid);
                            reactionsBar.setStyle(
                                    "-fx-background-color: rgba(0,0,0,0.2); -fx-background-radius: 10; -fx-padding: 2 5 2 5;");
                            reactionsBar.setMaxWidth(Region.USE_PREF_SIZE);
                            bubbleContent.getChildren().add(bubbleContent.getChildren().size() - 1, reactionsBar);
                        }

                        Label reactionLabel = null;
                        for (javafx.scene.Node rNode : reactionsBar.getChildren()) {
                            if (userId.equals(rNode.getUserData())) {
                                reactionLabel = (Label) rNode;
                                break;
                            }
                        }
                        if (reactionLabel == null) {
                            reactionLabel = new Label(emoji);
                            reactionLabel.setUserData(userId);
                            reactionLabel.setStyle("-fx-font-size: 12;");
                            reactionsBar.getChildren().add(reactionLabel);
                        } else {
                            reactionLabel.setText(emoji);
                        }
                    }
                    break;
                }
            }
        });
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
                switch (msg.getStatus()) {
                    case SENDING:
                        tickStr = " 🕒";
                        break;
                    case SENT_TO_SERVER:
                        tickStr = " ✓";
                        break;
                    case DELIVERED_TO_CLIENT:
                        tickStr = " ✓✓";
                        break;
                    case READ:
                        tickStr = " ✓✓";
                        break;
                }
            }

            Label timeLabel = new Label(timeStr + tickStr);
            timeLabel.setFont(javafx.scene.text.Font.font("System", 11));
            timeLabel.setTextFill(msg.getStatus() == com.messenger.common.Message.MessageStatus.READ && isMe
                    ? javafx.scene.paint.Color.web("#53bdeb")
                    : javafx.scene.paint.Color.web("#8696a0"));

            VBox mainContent = new VBox(5);

            if (msg.getGroupId() != null && !isMe) {
                Label senderNameLbl = new Label(msg.getSender() != null ? msg.getSender().getUsername() : "Group Member");
                int hash = (msg.getSender() != null ? msg.getSender().getUsername() : "Group Member").hashCode();
                int hue = Math.abs(hash % 360);
                senderNameLbl.setStyle("-fx-text-fill: hsb(" + hue + ", 75%, 85%); -fx-font-weight: bold; -fx-font-size: 13;");
                mainContent.getChildren().add(senderNameLbl);
            }

            if (msg.getLinkTitle() != null) {
                VBox linkCard = new VBox(5);
                linkCard.setStyle("-fx-background-color: rgba(0,0,0,0.2); -fx-background-radius: 5; -fx-padding: 8;");
                linkCard.setPrefWidth(250);
                linkCard.setCursor(javafx.scene.Cursor.HAND);
                linkCard.setOnMouseClicked(e -> {
                    String url = com.messenger.client.util.LinkPreviewService.extractUrl(msg.getContent());
                    if (url != null) {
                        new Thread(() -> {
                            try {
                                java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }).start();
                    }
                });

                if (msg.getLinkImageUrl() != null && !msg.getLinkImageUrl().isEmpty()) {
                    javafx.scene.image.ImageView linkImg = new javafx.scene.image.ImageView(
                            new javafx.scene.image.Image(msg.getLinkImageUrl(), 250, 150, true, true, true));
                    linkCard.getChildren().add(linkImg);
                }

                Label lTitle = new Label(msg.getLinkTitle());
                lTitle.setStyle("-fx-text-fill: #e9edef; -fx-font-weight: bold; -fx-font-size: 13;");
                lTitle.setWrapText(true);

                Label lDesc = new Label(msg.getLinkDescription());
                lDesc.setStyle("-fx-text-fill: #8696a0; -fx-font-size: 12;");
                lDesc.setWrapText(true);
                lDesc.setMaxHeight(50);

                linkCard.getChildren().addAll(lTitle, lDesc);
                mainContent.getChildren().add(linkCard);
            }

            if (msg.getFileData() != null) {
                String fname = msg.getFileName().toLowerCase();
                if (fname.endsWith(".png") || fname.endsWith(".jpg") || fname.endsWith(".jpeg")
                        || fname.endsWith(".gif") || fname.endsWith(".webp")) {
                    javafx.scene.image.Image img = new javafx.scene.image.Image(
                            new java.io.ByteArrayInputStream(msg.getFileData()));
                    javafx.scene.image.ImageView imgView = new javafx.scene.image.ImageView(img);
                    imgView.setFitWidth(250);
                    imgView.setPreserveRatio(true);

                    Button imgDlBtn = new Button("⬇ Download Image");
                    imgDlBtn.setStyle(
                            "-fx-background-color: rgba(0,0,0,0.3); -fx-text-fill: white; -fx-font-size: 10; -fx-cursor: hand; -fx-background-radius: 5; -fx-padding: 2 5 2 5;");
                    imgDlBtn.setOnAction(e -> {
                        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
                        fc.setInitialFileName(msg.getFileName());
                        java.io.File dest = fc.showSaveDialog(view.getScene().getWindow());
                        if (dest != null) {
                            try {
                                java.nio.file.Files.write(dest.toPath(), msg.getFileData());
                                addSystemMessage("Saved " + msg.getFileName());
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    });

                    mainContent.getChildren().addAll(imgView, imgDlBtn);
                } else if (msg.getFileName().toLowerCase().endsWith(".wav")) {
                    HBox audioBox = new HBox(10);
                    audioBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    audioBox.setStyle(
                            "-fx-background-color: rgba(0,0,0,0.1); -fx-padding: 8; -fx-background-radius: 5;");

                    Button playBtn = new Button("▶");
                    playBtn.setStyle(
                            "-fx-background-color: #00a884; -fx-text-fill: white; -fx-background-radius: 50; -fx-min-width: 30; -fx-min-height: 30; -fx-cursor: hand;");

                    ProgressBar progress = new ProgressBar(0);
                    progress.setPrefWidth(150);
                    progress.setStyle("-fx-accent: #00a884;");

                    playBtn.setOnAction(e -> {
                        try {
                            java.io.File temp = java.io.File.createTempFile("voice_", ".wav");
                            temp.deleteOnExit();
                            java.nio.file.Files.write(temp.toPath(), msg.getFileData());

                            javafx.scene.media.Media media = new javafx.scene.media.Media(temp.toURI().toString());
                            javafx.scene.media.MediaPlayer player = new javafx.scene.media.MediaPlayer(media);
                            player.currentTimeProperty().addListener((obs, oldV, newV) -> {
                                if (media.getDuration() != null)
                                    progress.setProgress(newV.toMillis() / media.getDuration().toMillis());
                            });
                            player.setOnEndOfMedia(() -> {
                                Platform.runLater(() -> {
                                    playBtn.setText("▶");
                                    progress.setProgress(0);
                                    player.dispose();
                                    if (temp.exists())
                                        temp.delete();
                                });
                            });

                            if (playBtn.getText().equals("▶")) {
                                player.play();
                                playBtn.setText("⏸");
                            } else {
                                player.pause();
                                playBtn.setText("▶");
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    });

                    audioBox.getChildren().addAll(playBtn, progress);
                    mainContent.getChildren().add(audioBox);
                } else {
                    HBox fileBox = new HBox(10);
                    fileBox.setStyle(
                            "-fx-background-color: rgba(255,255,255,0.05); -fx-padding: 10; -fx-background-radius: 5;");
                    fileBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                    javafx.scene.shape.SVGPath fileIcon = new javafx.scene.shape.SVGPath();
                    fileIcon.setContent(
                            "M6,2C4.89,2,4.01,2.89,4.01,4L4,20c0,1.1,0.89,2,1.99,2H18c1.1,0,2-0.9,2-2V8l-6-6H6z M13,9V3.5L18.5,9H13z");
                    fileIcon.setFill(javafx.scene.paint.Color.web("#e9edef"));

                    Label fNameLabel = new Label(msg.getFileName());
                    fNameLabel.setTextFill(javafx.scene.paint.Color.web("#e9edef"));
                    fNameLabel.setWrapText(true);
                    fNameLabel.setMaxWidth(150);

                    Button dlBtn = new Button("⬇");
                    dlBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #00a884; -fx-cursor: hand;");
                    dlBtn.setOnAction(e -> {
                        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
                        fc.setInitialFileName(msg.getFileName());
                        java.io.File dest = fc.showSaveDialog(view.getScene().getWindow());
                        if (dest != null) {
                            try {
                                java.nio.file.Files.write(dest.toPath(), msg.getFileData());
                                addSystemMessage("Saved " + msg.getFileName());
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    });

                    fileBox.getChildren().addAll(fileIcon, fNameLabel, dlBtn);
                    mainContent.getChildren().add(fileBox);
                }
            }

            if (msg.getContent() != null && !msg.getContent().isEmpty()) {
                mainContent.getChildren().add(msgLabel);
            }

            if (msg.getParentMessageId() != null) {
                VBox parentBox = new VBox(2);
                parentBox.setStyle(
                        "-fx-background-color: rgba(0,0,0,0.15); -fx-border-color: #00a884; -fx-border-width: 0 0 0 3; -fx-padding: 5; -fx-background-radius: 3; -fx-border-radius: 3;");
                Label pText = new Label(
                        msg.getParentMessageContent() != null ? msg.getParentMessageContent() : "Message");
                pText.setStyle("-fx-text-fill: #8696a0; -fx-font-size: 13;");
                pText.setWrapText(true);
                pText.setMaxWidth(350);
                parentBox.getChildren().add(pText);
                mainContent.getChildren().add(0, parentBox);
            }

            javafx.scene.control.ContextMenu contextMenu = new javafx.scene.control.ContextMenu();

            javafx.scene.control.Menu reactMenu = new javafx.scene.control.Menu("React");
            String[] emojis = { "👍", "❤️", "😂", "😮", "😢", "🙏" };
            for (String emoji : emojis) {
                javafx.scene.control.MenuItem item = new javafx.scene.control.MenuItem(emoji);
                item.setOnAction(e -> {
                    chatManager.addReaction(msg.getId(), emoji,
                            isMe ? msg.getReceiver().getId() : msg.getSender().getId());
                    updateMessageReaction(msg.getId(), chatManager.getCurrentUser().getId(), emoji);
                });
                reactMenu.getItems().add(item);
            }
            contextMenu.getItems().add(reactMenu);

            javafx.scene.control.MenuItem replyItem = new javafx.scene.control.MenuItem("Reply");
            replyItem.setOnAction(e -> startReply(msg));
            contextMenu.getItems().add(replyItem);

            if (isMe) {
                javafx.scene.control.MenuItem deleteItem = new javafx.scene.control.MenuItem("Delete for Everyone");
                deleteItem.setOnAction(e -> {
                    chatManager.deleteMessage(msg.getId(), msg.getReceiver().getId());
                    updateMessageAsDeleted(msg.getId());
                });
                contextMenu.getItems().add(deleteItem);
            }

            VBox bubbleContent = new VBox(2, mainContent, timeLabel);

            if (msg.getReactions() != null && !msg.getReactions().isEmpty()) {
                HBox reactionsBar = new HBox(3);
                reactionsBar.setId("reactions_" + msg.getId());
                reactionsBar.setStyle(
                        "-fx-background-color: rgba(0,0,0,0.2); -fx-background-radius: 10; -fx-padding: 2 5 2 5;");
                reactionsBar.setMaxWidth(Region.USE_PREF_SIZE);
                msg.getReactions().forEach((uId, emoji) -> {
                    Label l = new Label(emoji);
                    l.setUserData(uId);
                    l.setStyle("-fx-font-size: 12;");
                    reactionsBar.getChildren().add(l);
                });
                bubbleContent.getChildren().add(1, reactionsBar);
            }
            bubbleContent.setAlignment(isMe ? javafx.geometry.Pos.CENTER_RIGHT : javafx.geometry.Pos.BOTTOM_RIGHT);
            bubbleContent.setPadding(new javafx.geometry.Insets(6, 10, 6, 10));
            bubbleContent.setMaxWidth(400); // Max bubble width
            bubbleContent
                    .setOnContextMenuRequested(e -> contextMenu.show(bubbleContent, e.getScreenX(), e.getScreenY()));

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

    private String formatLastSeen(java.time.LocalDateTime dt) {
        if (dt == null)
            return "offline";
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate thatDay = dt.toLocalDate();
        java.time.format.DateTimeFormatter timeFmt = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
        if (today.equals(thatDay))
            return "last seen today at " + timeFmt.format(dt);
        if (today.minusDays(1).equals(thatDay))
            return "last seen yesterday at " + timeFmt.format(dt);
        return "last seen " + java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(dt);
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
            phoneIcon.setContent(
                    "M18.48,15.76C18.48,15.76,16.59,14.65,16.14,14.45C15.68,14.24,15.25,14.36,14.97,14.73C14.68,15.11,13.88,16.09,13.6,16.42C13.31,16.74,13.01,16.78,12.56,16.55C12.1,16.32,10.65,15.22,9.33,13.78C8.28,12.63,7.57,11.13,7.29,10.67C7.01,10.21,7.26,9.96,7.49,9.73C7.69,9.53,7.94,9.22,8.17,8.96C8.4,8.7,8.48,8.51,8.63,8.21C8.78,7.91,8.71,7.63,8.59,7.4C8.48,7.17,7.78,5.46,7.48,4.72C7.2,4.01,6.91,4.1,6.7,4.09C6.51,4.08,6.21,4.08,5.9,4.08C5.6,4.08,5.11,4.19,4.69,4.64C4.28,5.09,3.14,6.15,3.14,8.32C3.14,10.5,4.74,12.58,4.96,12.89C5.19,13.19,8.08,17.65,12.51,19.56C13.56,20.02,14.39,20.29,15.03,20.5C16.08,20.83,17.03,20.78,17.78,20.67C18.61,20.55,20.35,19.64,20.72,18.63C21.1,17.62,21.1,16.74,20.98,16.55C20.87,16.36,20.57,16.24,20.11,16.01L18.48,15.76Z");
            phoneIcon.setFill(
                    missed ? javafx.scene.paint.Color.web("#f15c6d") : javafx.scene.paint.Color.web("#00a884"));
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

    private void showTypingIndicator(boolean show) {
        if (show) {
            if (typingIndicatorBubble == null) {
                Label dots = new Label("...");
                dots.setStyle("-fx-text-fill: #e9edef; -fx-font-size: 24; -fx-font-weight: bold;");

                HBox bubble = new HBox(dots);
                bubble.setStyle(
                        "-fx-background-color: #202c33; -fx-background-radius: 0 15 15 15; -fx-padding: 0 15 5 15;");
                bubble.setAlignment(javafx.geometry.Pos.CENTER);

                typingIndicatorBubble = new HBox(bubble);
                typingIndicatorBubble.setPadding(new javafx.geometry.Insets(5, 20, 5, 20));
                typingIndicatorBubble.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            }
            if (!chatArea.getChildren().contains(typingIndicatorBubble)) {
                chatArea.getChildren().add(typingIndicatorBubble);
            }
        } else {
            if (typingIndicatorBubble != null) {
                chatArea.getChildren().remove(typingIndicatorBubble);
            }
        }
    }

    private void showLinkPreview(com.messenger.client.util.LinkPreviewService.LinkMetadata meta) {
        linkPreviewArea.getChildren().clear();
        HBox card = new HBox(10);
        card.setStyle("-fx-background-color: #202c33; -fx-background-radius: 8; -fx-padding: 10;");
        card.setPrefHeight(60);

        if (meta.imageUrl != null && !meta.imageUrl.isEmpty()) {
            javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(
                    new javafx.scene.image.Image(meta.imageUrl, 50, 50, true, true, true));
            card.getChildren().add(iv);
        }

        VBox texts = new VBox(2);
        Label t = new Label(meta.title);
        t.setStyle("-fx-text-fill: #e9edef; -fx-font-weight: bold; -fx-font-size: 13;");
        Label d = new Label(meta.description);
        d.setStyle("-fx-text-fill: #8696a0; -fx-font-size: 11;");
        d.setWrapText(false);
        d.setMaxWidth(400);

        texts.getChildren().addAll(t, d);
        card.getChildren().add(texts);

        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #8696a0; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> {
            currentLinkMetadata = null;
            linkPreviewArea.getChildren().clear();
        });

        HBox layout = new HBox(card, closeBtn);
        HBox.setHgrow(card, Priority.ALWAYS);
        linkPreviewArea.getChildren().add(layout);
    }

    private void updateUnifiedSidebar() {
        Platform.runLater(() -> {
            java.util.List<Object> combined = new java.util.ArrayList<>();
            combined.addAll(localGroups);
            combined.addAll(localContacts);
            usersList.setItems(FXCollections.observableArrayList(combined));
        });
    }

    private void openNewGroupModal() {
        javafx.stage.Stage modal = new javafx.stage.Stage();
        modal.initOwner(view.getScene().getWindow());
        modal.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        modal.setTitle("Create New Group");

        VBox layout = new VBox(15);
        layout.setPadding(new javafx.geometry.Insets(20));
        layout.setStyle("-fx-background-color: #111b21;");

        Label title = new Label("Create Group");
        title.setStyle("-fx-text-fill: #00a884; -fx-font-size: 18; -fx-font-weight: bold;");

        TextField nameField = new TextField();
        nameField.setPromptText("Group Name");
        nameField.setStyle("-fx-background-color: #2a3942; -fx-text-fill: white; -fx-prompt-text-fill: #8696a0; -fx-background-radius: 8; -fx-padding: 8;");

        TextField descField = new TextField();
        descField.setPromptText("Description (Optional)");
        descField.setStyle("-fx-background-color: #2a3942; -fx-text-fill: white; -fx-prompt-text-fill: #8696a0; -fx-background-radius: 8; -fx-padding: 8;");

        Label membersLabel = new Label("Select Members:");
        membersLabel.setStyle("-fx-text-fill: #e9edef; -fx-font-size: 14; -fx-font-weight: bold;");

        ListView<javafx.scene.layout.BorderPane> contactsSelectionList = new ListView<>();
        contactsSelectionList.setStyle("-fx-background-color: #111b21; -fx-control-inner-background: #111b21;");
        contactsSelectionList.setPrefHeight(200);

        java.util.List<javafx.scene.control.CheckBox> checkBoxes = new java.util.ArrayList<>();
        for (User user : localContacts) {
            javafx.scene.layout.BorderPane cell = new javafx.scene.layout.BorderPane();
            Label name = new Label(user.getUsername() + " (" + user.getPhoneNumber() + ")");
            name.setStyle("-fx-text-fill: #e9edef;");
            
            javafx.scene.control.CheckBox cb = new javafx.scene.control.CheckBox();
            cb.setUserData(user.getId());
            cb.setStyle("-fx-accent: #00a884;");
            checkBoxes.add(cb);
            
            cell.setLeft(name);
            cell.setRight(cb);
            contactsSelectionList.getItems().add(cell);
        }

        Button createBtn = new Button("CREATE GROUP");
        createBtn.setStyle("-fx-background-color: #00a884; -fx-text-fill: #111b21; -fx-font-weight: bold; -fx-font-size: 14; -fx-background-radius: 8; -fx-padding: 10 20 10 20; -fx-cursor: hand;");
        createBtn.setMaxWidth(Double.MAX_VALUE);
        createBtn.setOnAction(ev -> {
            String name = nameField.getText().trim();
            String desc = descField.getText().trim();
            if (name.isEmpty()) {
                return;
            }

            java.util.List<String> selectedIds = new java.util.ArrayList<>();
            for (javafx.scene.control.CheckBox cb : checkBoxes) {
                if (cb.isSelected()) {
                    selectedIds.add((String) cb.getUserData());
                }
            }

            chatManager.createGroup(name, desc, selectedIds);
            modal.close();
        });

        layout.getChildren().addAll(title, nameField, descField, membersLabel, contactsSelectionList, createBtn);
        modal.setScene(new javafx.scene.Scene(layout, 350, 480));
        modal.showAndWait();
    }

    private void checkAndShowContactBanner() {
        if (selectedUser != null) {
            boolean isKnown = false;
            for (User u : localContacts) {
                if (u.getId().equals(selectedUser.getId()) && "ACCEPTED".equals(u.getRelationshipStatus())) {
                    isKnown = true;
                    break;
                }
            }
            if (!isKnown) {
                VBox banner = new VBox(10);
                banner.setId("contact_banner");
                banner.setStyle("-fx-background-color: #202c33; -fx-padding: 15; -fx-background-radius: 8;");
                banner.setAlignment(javafx.geometry.Pos.CENTER);
                
                Label warning = new Label("The sender is not in your contacts list.");
                warning.setTextFill(javafx.scene.paint.Color.web("#e9edef"));
                warning.setFont(javafx.scene.text.Font.font("System", 14));
                
                HBox buttons = new HBox(20);
                buttons.setAlignment(javafx.geometry.Pos.CENTER);
                
                Button blockBtn = new Button("Block");
                blockBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #f15c6d; -fx-border-color: #f15c6d; -fx-border-radius: 4; -fx-padding: 5 15; -fx-cursor: hand;");
                
                Button acceptBtn = new Button("Accept / Add to Contacts");
                acceptBtn.setStyle("-fx-background-color: #00a884; -fx-text-fill: #111b21; -fx-font-weight: bold; -fx-background-radius: 4; -fx-padding: 5 15; -fx-cursor: hand;");
                
                blockBtn.setOnAction(e -> {
                    chatManager.blockUser(selectedUser.getId());
                    chatArea.getChildren().remove(banner);
                });
                
                acceptBtn.setOnAction(e -> {
                    chatManager.acceptUser(selectedUser.getId());
                    chatArea.getChildren().remove(banner);
                });
                
                buttons.getChildren().addAll(blockBtn, acceptBtn);
                banner.getChildren().addAll(warning, buttons);
                
                chatArea.getChildren().add(banner);
            }
        }
    }
    private javafx.scene.layout.StackPane createAvatarWithSilhouette(double radius) {
        javafx.scene.layout.StackPane stack = new javafx.scene.layout.StackPane();
        javafx.scene.shape.Circle background = new javafx.scene.shape.Circle(radius, javafx.scene.paint.Color.web("#dfe5e7"));
        
        javafx.scene.shape.SVGPath silhouette = new javafx.scene.shape.SVGPath();
        silhouette.setContent("M12,2C6.48,2,2,6.48,2,12s4.48,10,10,10s10-4.48,10-10S17.52,2,12,2z M12,5c1.66,0,3,1.34,3,3s-1.34,3-3,3s-3-1.34-3-3S10.34,5,12,5z M12,19.2c-2.5,0-4.71-1.28-6-3.22c0.03-1.99,4-3.08,6-3.08c1.99,0,5.97,1.09,6,3.08C16.71,17.92,14.5,19.2,12,19.2z");
        silhouette.setFill(javafx.scene.paint.Color.web("#ffffff"));
        double scale = radius / 12.0; 
        silhouette.setScaleX(scale);
        silhouette.setScaleY(scale);
        
        stack.getChildren().addAll(background, silhouette);
        stack.getProperties().put("background", background);
        stack.getProperties().put("silhouette", silhouette);
        return stack;
    }

    private void applyAvatarImage(javafx.scene.layout.StackPane avatarStack, byte[] imageData) {
        javafx.scene.shape.Circle background = (javafx.scene.shape.Circle) avatarStack.getProperties().get("background");
        javafx.scene.shape.SVGPath silhouette = (javafx.scene.shape.SVGPath) avatarStack.getProperties().get("silhouette");
        
        if (imageData != null && imageData.length > 0) {
            try {
                java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(imageData);
                javafx.scene.image.Image img = new javafx.scene.image.Image(bis);
                if (!img.isError()) {
                    background.setFill(new javafx.scene.paint.ImagePattern(img));
                    silhouette.setVisible(false);
                } else {
                    background.setFill(javafx.scene.paint.Color.web("#dfe5e7"));
                    silhouette.setVisible(true);
                }
            } catch (Exception e) {
                background.setFill(javafx.scene.paint.Color.web("#dfe5e7"));
                silhouette.setVisible(true);
            }
        } else {
            background.setFill(javafx.scene.paint.Color.web("#dfe5e7"));
            silhouette.setVisible(true);
        }
    }
}
