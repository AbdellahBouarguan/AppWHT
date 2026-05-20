package com.messenger.client.ui;

import com.messenger.client.ChatManager;
import com.messenger.client.media.GroupMediaCapture;
import com.messenger.client.media.GroupStreamReceiver;
import com.messenger.client.media.StreamSender;
import com.messenger.common.User;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A state-of-the-art, premium JavaFX Group Calling UI with sleek dark glassmorphic styling,
 * dynamic auto-scaling grid, visual audio visualizers, and interactive controls.
 */
public class GroupCallView {
    private final String groupId;
    private final String groupName;
    private final ChatManager chatManager;
    private final String serverIp;
    private final int audioPort;
    private final int videoPort;
    
    private Stage stage;
    private FlowPane gridPane;
    private Label callTitleLabel;
    
    private GroupStreamReceiver receiver;
    private GroupMediaCapture mediaCapture;
    
    private final Map<String, ParticipantCard> participantCards = new HashMap<>();
    private final List<User> initialParticipants;
    
    private boolean isMuted = false;
    private boolean isCameraOff = false;
    
    private Button muteBtn;
    private Button camBtn;
    
    private Runnable onCloseHandler;

    private static class ParticipantCard extends VBox {
        final String userId;
        final ImageView imageView;
        final StackPane avatarStack;
        final Label nameLabel;
        final ProgressBar volumeBar;
        final Label cameraOffLabel;
        
        ParticipantCard(String userId, String username, boolean isLocal) {
            this.userId = userId;
            this.setAlignment(Pos.CENTER);
            this.setSpacing(8);
            this.setPadding(new Insets(12));
            
            // Premium Card styling
            this.setStyle("-fx-background-color: rgba(17, 27, 33, 0.85);" +
                          "-fx-background-radius: 16;" +
                          "-fx-border-color: rgba(34, 46, 53, 0.5);" +
                          "-fx-border-width: 1;" +
                          "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 4);");
            
            // Set fixed dimensions for video card cells to look uniform
            this.setPrefSize(200, 200);

            // Container for image view and fallback avatar
            StackPane mediaContainer = new StackPane();
            mediaContainer.setPrefSize(140, 110);
            mediaContainer.setStyle("-fx-background-color: #0b141a; -fx-background-radius: 12;");
            
            imageView = new ImageView();
            imageView.setFitWidth(140);
            imageView.setFitHeight(110);
            imageView.setPreserveRatio(true);
            
            // Fallback avatar circle
            avatarStack = new StackPane();
            Circle circle = new Circle(32);
            // Dynamic colorful backgrounds based on username to make it look premium
            int colorHash = Math.abs(username.hashCode() % 5);
            String[] bgColors = {"#25D366", "#34B7F1", "#ea0038", "#e2a300", "#a855f7"};
            circle.setStyle("-fx-fill: " + bgColors[colorHash] + ";");
            
            Label initials = new Label(username.substring(0, Math.min(2, username.length())).toUpperCase());
            initials.setStyle("-fx-font-family: 'Outfit', 'Segoe UI', sans-serif; -fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: white;");
            avatarStack.getChildren().addAll(circle, initials);
            
            cameraOffLabel = new Label("Camera Off");
            cameraOffLabel.setStyle("-fx-font-family: 'Segoe UI', sans-serif; -fx-font-size: 11; -fx-text-fill: #8696a0;");
            cameraOffLabel.setVisible(false);
            
            mediaContainer.getChildren().addAll(avatarStack, imageView, cameraOffLabel);

            // Details
            nameLabel = new Label(username + (isLocal ? " (You)" : ""));
            nameLabel.setStyle("-fx-font-family: 'Segoe UI', sans-serif; -fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: white;");
            
            volumeBar = new ProgressBar(0.0);
            volumeBar.setPrefWidth(120);
            volumeBar.setPrefHeight(6);
            volumeBar.setStyle("-fx-accent: #25D366; -fx-control-inner-background: #222e35; -fx-background-radius: 3; -fx-padding: 0;");
            
            this.getChildren().addAll(mediaContainer, nameLabel, volumeBar);
        }

        void updateVideoFrame(byte[] jpgBytes) {
            if (jpgBytes == null || jpgBytes.length == 0) {
                // Camera Off
                imageView.setVisible(false);
                avatarStack.setVisible(true);
                cameraOffLabel.setVisible(true);
            } else {
                try {
                    Image img = new Image(new ByteArrayInputStream(jpgBytes));
                    imageView.setImage(img);
                    imageView.setVisible(true);
                    avatarStack.setVisible(false);
                    cameraOffLabel.setVisible(false);
                } catch (Exception ignored) {}
            }
        }

        void setVolumeLevel(double amplitude) {
            // Smooth dynamic green indicator representing speaker amplitude
            volumeBar.setProgress(amplitude);
        }
    }

    public GroupCallView(String groupId, String groupName, ChatManager chatManager, 
                         String serverIp, int audioPort, int videoPort, List<User> initialParticipants) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.chatManager = chatManager;
        this.serverIp = serverIp;
        this.audioPort = audioPort;
        this.videoPort = videoPort;
        this.initialParticipants = initialParticipants;
    }

    public void show() {
        stage = new Stage();
        stage.setTitle("Group Call - " + groupName);
        stage.setResizable(false);

        // Parent container (Modern WhatsApp deep charcoal tone background)
        VBox root = new VBox();
        root.setAlignment(Pos.TOP_CENTER);
        root.setPadding(new Insets(20));
        root.setSpacing(20);
        root.setStyle("-fx-background-color: #0b141a;");

        // Premium Title banner
        callTitleLabel = new Label("Group Call: " + groupName);
        callTitleLabel.setStyle("-fx-font-family: 'Segoe UI', sans-serif; -fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #e9edef;");
        
        Label memberCountLabel = new Label(initialParticipants.size() + " participants active");
        memberCountLabel.setStyle("-fx-font-family: 'Segoe UI', sans-serif; -fx-font-size: 13; -fx-text-fill: #8696a0;");

        VBox titleBox = new VBox(4, callTitleLabel, memberCountLabel);
        titleBox.setAlignment(Pos.CENTER);

        // Fluid video/avatar grid
        gridPane = new FlowPane();
        gridPane.setHgap(15);
        gridPane.setVgap(15);
        gridPane.setPadding(new Insets(10));
        gridPane.setAlignment(Pos.CENTER);
        gridPane.setPrefWrapLength(700);
        gridPane.setStyle("-fx-background-color: transparent;");

        // Glassmorphic control box at the bottom
        HBox controlsBox = new HBox(20);
        controlsBox.setAlignment(Pos.CENTER);
        controlsBox.setPadding(new Insets(15, 30, 15, 30));
        controlsBox.setStyle("-fx-background-color: rgba(34, 46, 53, 0.7);" +
                             "-fx-background-radius: 24;" +
                             "-fx-border-color: rgba(255,255,255,0.08);" +
                             "-fx-border-width: 1;");
        controlsBox.setMaxWidth(400);

        // Toggle buttons and End Call button
        muteBtn = createStyledButton("🎤 Mute", "#222e35", "#e9edef");
        muteBtn.setOnAction(e -> toggleMute());

        camBtn = createStyledButton("📷 Cam Off", "#222e35", "#e9edef");
        camBtn.setOnAction(e -> toggleCamera());

        Button hangUpBtn = createStyledButton("🛑 Leave", "#ea0038", "white");
        hangUpBtn.setOnAction(e -> hangUp());

        controlsBox.getChildren().addAll(muteBtn, camBtn, hangUpBtn);

        root.getChildren().addAll(titleBox, gridPane, controlsBox);

        Scene scene = new Scene(root, 720, 600);
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> hangUp()); // Clean leave on window close
        stage.show();

        // 1. Setup multi-media receiver pipeline
        setupMediaPipeline();
        
        // 2. Add ourselves and initial participants to UI grid
        initializeGrid();
    }

    private Button createStyledButton(String text, String bgHex, String textHex) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + bgHex + ";" +
                     "-fx-text-fill: " + textHex + ";" +
                     "-fx-font-family: 'Segoe UI', sans-serif;" +
                     "-fx-font-size: 13;" +
                     "-fx-font-weight: bold;" +
                     "-fx-background-radius: 14;" +
                     "-fx-padding: 8 16 8 16;" +
                     "-fx-cursor: hand;" +
                     "-fx-transition: all 0.2s ease-out;");
        
        // Hover effects
        btn.setOnMouseEntered(e -> btn.setOpacity(0.85));
        btn.setOnMouseExited(e -> btn.setOpacity(1.0));
        return btn;
    }

    private void setupMediaPipeline() {
        // Create the stream receiver
        receiver = new GroupStreamReceiver();
        receiver.start(serverIp, audioPort, videoPort);
        
        // Handle incoming remote video frames
        receiver.setVideoFrameConsumer((senderId, frameData) -> {
            Platform.runLater(() -> {
                ParticipantCard card = participantCards.get(senderId);
                if (card != null) {
                    card.updateVideoFrame(frameData);
                }
            });
        });
        
        // Handle incoming remote audio amplitudes
        receiver.setAudioAmplitudeConsumer((senderId, amplitude) -> {
            Platform.runLater(() -> {
                ParticipantCard card = participantCards.get(senderId);
                if (card != null) {
                    card.setVolumeLevel(amplitude);
                }
            });
        });

        // Use the receiver's socket for symmetric UDP hole punching
        mediaCapture = new GroupMediaCapture(chatManager.getCurrentUser().getId(), receiver::sendAudio, receiver::sendVideo);
        
        // Run capture pipeline, displaying local webcam feed in our card
        mediaCapture.startCapture(
            localFrame -> Platform.runLater(() -> {
                ParticipantCard localCard = participantCards.get(chatManager.getCurrentUser().getId());
                if (localCard != null) {
                    localCard.updateVideoFrame(localFrame);
                }
            }),
            localAmplitude -> Platform.runLater(() -> {
                ParticipantCard localCard = participantCards.get(chatManager.getCurrentUser().getId());
                if (localCard != null) {
                    localCard.setVolumeLevel(localAmplitude);
                }
            })
        );
    }

    private void initializeGrid() {
        gridPane.getChildren().clear();
        participantCards.clear();

        // 1. Add ourselves first
        User self = chatManager.getCurrentUser();
        ParticipantCard selfCard = new ParticipantCard(self.getId(), self.getUsername(), true);
        participantCards.put(self.getId(), selfCard);
        gridPane.getChildren().add(selfCard);

        // 2. Add existing call participants
        for (User user : initialParticipants) {
            if (user.getId().equals(self.getId())) continue; // Skip since self is added
            ParticipantCard card = new ParticipantCard(user.getId(), user.getUsername(), false);
            participantCards.put(user.getId(), card);
            gridPane.getChildren().add(card);
        }
    }

    public void updateParticipants(List<User> updatedList) {
        Platform.runLater(() -> {
            User self = chatManager.getCurrentUser();
            
            // Remove left participants
            participantCards.keySet().removeIf(userId -> {
                if (userId.equals(self.getId())) return false; // Never remove ourselves
                
                boolean stillPresent = false;
                for (User u : updatedList) {
                    if (u.getId().equals(userId)) {
                        stillPresent = true;
                        break;
                    }
                }
                
                if (!stillPresent) {
                    ParticipantCard card = participantCards.get(userId);
                    if (card != null) {
                        gridPane.getChildren().remove(card);
                    }
                    return true;
                }
                return false;
            });

            // Add newly joined participants
            for (User u : updatedList) {
                if (u.getId().equals(self.getId())) continue;
                if (!participantCards.containsKey(u.getId())) {
                    ParticipantCard card = new ParticipantCard(u.getId(), u.getUsername(), false);
                    participantCards.put(u.getId(), card);
                    gridPane.getChildren().add(card);
                }
            }
        });
    }

    private void toggleMute() {
        isMuted = !isMuted;
        mediaCapture.setAudioEnabled(!isMuted);
        muteBtn.setText(isMuted ? "🔇 Unmute" : "🎤 Mute");
        muteBtn.setStyle(isMuted 
            ? "-fx-background-color: #ea0038; -fx-text-fill: white; -fx-font-family: 'Segoe UI', sans-serif; -fx-font-size: 13; -fx-font-weight: bold; -fx-background-radius: 14; -fx-padding: 8 16 8 16;"
            : "-fx-background-color: #222e35; -fx-text-fill: #e9edef; -fx-font-family: 'Segoe UI', sans-serif; -fx-font-size: 13; -fx-font-weight: bold; -fx-background-radius: 14; -fx-padding: 8 16 8 16;"
        );
        
        // Instantly reset local amplitude bar on mute
        ParticipantCard selfCard = participantCards.get(chatManager.getCurrentUser().getId());
        if (selfCard != null && isMuted) {
            selfCard.setVolumeLevel(0.0);
        }
    }

    private void toggleCamera() {
        isCameraOff = !isCameraOff;
        mediaCapture.setVideoEnabled(!isCameraOff);
        camBtn.setText(isCameraOff ? "📷 Cam On" : "📷 Cam Off");
        camBtn.setStyle(isCameraOff 
            ? "-fx-background-color: #ea0038; -fx-text-fill: white; -fx-font-family: 'Segoe UI', sans-serif; -fx-font-size: 13; -fx-font-weight: bold; -fx-background-radius: 14; -fx-padding: 8 16 8 16;"
            : "-fx-background-color: #222e35; -fx-text-fill: #e9edef; -fx-font-family: 'Segoe UI', sans-serif; -fx-font-size: 13; -fx-font-weight: bold; -fx-background-radius: 14; -fx-padding: 8 16 8 16;"
        );
        
        // Instantly reset local image view display on camera toggle
        ParticipantCard selfCard = participantCards.get(chatManager.getCurrentUser().getId());
        if (selfCard != null) {
            selfCard.updateVideoFrame(null); // Shows avatar card fallback
        }
    }

    private void hangUp() {
        System.out.println("Hanging up group call...");
        if (mediaCapture != null) {
            mediaCapture.stopCapture();
        }
        if (receiver != null) {
            receiver.stop();
        }
        
        chatManager.leaveGroupCall(groupId);
        
        if (stage != null) {
            stage.close();
        }
        
        if (onCloseHandler != null) {
            onCloseHandler.run();
        }
    }

    public void setOnClose(Runnable onCloseHandler) {
        this.onCloseHandler = onCloseHandler;
    }

    public String getGroupId() {
        return groupId;
    }
}
