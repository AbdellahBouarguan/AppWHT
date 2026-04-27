package com.messenger.client;

import com.messenger.common.CallType;
import com.messenger.common.Message;
import com.messenger.common.NetworkPayload;
import com.messenger.common.User;
import javafx.application.Platform;

import java.util.List;

public class ChatManager {
    // Callbacks to UI
    private Runnable onLoginSuccess;
    private Runnable onLoginFailed;
    private MessageListener messageListener;
    private UserListListener userListListener;
    private CallListener callListener;

    private NetworkClient networkClient;
    private User currentUser;

    public ChatManager() {
    }

    public void connectAndLogin(String ip, int port, String username, String password) {
        if (networkClient == null) {
            networkClient = new NetworkClient(ip, port, this);
            boolean connected = networkClient.connect();
            if (!connected) {
                if (onLoginFailed != null)
                    Platform.runLater(onLoginFailed);
                return;
            }
        }
        networkClient.login(username, password);
    }

    public void sendMessage(User receiver, String text) {
        Message msg = new Message(currentUser, receiver, text);
        networkClient.sendData(new NetworkPayload(NetworkPayload.PayloadType.MESSAGE, msg));
    }

    public void requestCall(User receiver, CallType type) {
        Object[] callData = new Object[] { receiver.getId(), type };
        networkClient.sendData(new NetworkPayload(NetworkPayload.PayloadType.CALL_REQUEST, callData));
    }

    public void setOnLoginSuccess(Runnable onLoginSuccess) {
        this.onLoginSuccess = onLoginSuccess;
    }

    public void setOnLoginFailed(Runnable onLoginFailed) {
        this.onLoginFailed = onLoginFailed;
    }

    public void setMessageListener(MessageListener messageListener) {
        this.messageListener = messageListener;
    }

    public void setUserListListener(UserListListener userListListener) {
        this.userListListener = userListListener;
    }

    public void setCallListener(CallListener callListener) {
        this.callListener = callListener;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    @SuppressWarnings("unchecked")
    public void handlePayload(NetworkPayload payload) {
        Platform.runLater(() -> {
            switch (payload.getType()) {
                case AUTH_RESPONSE:
                    if ("OK".equals(payload.getStatus())) {
                        currentUser = (User) payload.getData();
                        if (onLoginSuccess != null)
                            onLoginSuccess.run();
                    } else {
                        if (onLoginFailed != null)
                            onLoginFailed.run();
                    }
                    break;
                case USERS_LIST_UPDATE:
                    if (userListListener != null) {
                        userListListener.onUserListUpdated((List<User>) payload.getData());
                    }
                    break;
                case MESSAGE:
                    if (messageListener != null) {
                        messageListener.onMessageReceived((Message) payload.getData());
                    }
                    break;
                case CALL_REQUEST:
                    if (callListener != null) {
                        // Data: [senderId as receiverId context in server route, type]
                        // Actually the server routed the payload directly, we should know the sender.
                        // For simplicity let's assume it routes it appropriately.
                        Object[] data = (Object[]) payload.getData();
                        callListener.onIncomingCall((String) data[0], (CallType) data[1]);
                    }
                    break;
                default:
                    break;
            }
        });
    }

    public interface MessageListener {
        void onMessageReceived(Message msg);
    }

    public interface UserListListener {
        void onUserListUpdated(List<User> users);
    }

    public interface CallListener {
        void onIncomingCall(String callerId, CallType type);
    }
}
