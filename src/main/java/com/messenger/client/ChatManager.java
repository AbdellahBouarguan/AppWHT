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
    private StatusUpdateListener statusUpdateListener;
    private ChatHistoryListener chatHistoryListener;

    private NetworkClient networkClient;
    private User currentUser;

    public ChatManager() {
    }

    public void connectAndAuth(String ip, int port, String username, String password, String phoneNumber,
            boolean isRegister) {
        if (networkClient == null) {
            networkClient = new NetworkClient(ip, port, this);
            boolean connected = networkClient.connect();
            if (!connected) {
                if (onLoginFailed != null)
                    Platform.runLater(onLoginFailed);
                return;
            }
        }
        if (isRegister) {
            networkClient.register(username, password, phoneNumber);
        } else {
            networkClient.login(username, password);
        }
    }

    public void fetchContacts() {
        if (networkClient != null) {
            networkClient.fetchContacts();
        }
    }

    public void addContact(String contactId) {
        if (networkClient != null) {
            networkClient.addContact(contactId);
        }
    }

    public void fetchChatHistory(User contact) {
        if (networkClient != null) {
            networkClient.sendData(
                    new NetworkPayload(NetworkPayload.PayloadType.FETCH_CHAT_HISTORY_REQUEST, contact.getId()));
        }
    }

    public void sendMessage(User receiver, String text) {
        Message msg = new Message(currentUser, receiver, text);
        networkClient.sendData(new NetworkPayload(NetworkPayload.PayloadType.SEND_MESSAGE, msg));
    }

    public void requestCall(User receiver, CallType type) {
        Object[] callData = new Object[] { receiver.getId(), type };
        networkClient.sendData(new NetworkPayload(NetworkPayload.PayloadType.CALL_REQUEST, callData));
    }

    public void acceptCall(String callerId, CallType type) {
        Object[] callData = new Object[] { callerId, type };
        networkClient.sendData(new NetworkPayload(NetworkPayload.PayloadType.CALL_ACCEPT, callData));
    }

    public void rejectCall(String callerId, CallType type) {
        Object[] callData = new Object[] { callerId, type };
        networkClient.sendData(new NetworkPayload(NetworkPayload.PayloadType.CALL_REJECT, callData));
    }

    public void endCall(User receiver) {
        Object[] callData = new Object[] { receiver.getId(), CallType.AUDIO }; // type doesn't matter for end
        networkClient.sendData(new NetworkPayload(NetworkPayload.PayloadType.END_CALL, callData));
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

    public void setStatusUpdateListener(StatusUpdateListener statusListener) {
        this.statusUpdateListener = statusListener;
    }

    public void setCallListener(CallListener callListener) {
        this.callListener = callListener;
    }

    public void setChatHistoryListener(ChatHistoryListener listener) {
        this.chatHistoryListener = listener;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    @SuppressWarnings("unchecked")
    public void handlePayload(NetworkPayload payload) {
        Platform.runLater(() -> {
            switch (payload.getType()) {
                case LOGIN_SUCCESS:
                case REGISTER_SUCCESS:
                    if ("OK".equals(payload.getStatus())) {
                        currentUser = (User) payload.getData();
                        if (onLoginSuccess != null)
                            onLoginSuccess.run();
                        fetchContacts(); // Automatically fetch contacts on auth success
                    } else {
                        if (onLoginFailed != null)
                            onLoginFailed.run();
                    }
                    break;
                case CONTACT_LIST_RESPONSE:
                    if (userListListener != null) {
                        userListListener.onUserListUpdated((List<User>) payload.getData());
                    }
                    break;
                case STATUS_UPDATE:
                    if (statusUpdateListener != null) {
                        statusUpdateListener.onStatusUpdated((User) payload.getData());
                    }
                    break;
                case FETCH_CHAT_HISTORY_RESPONSE:
                    if (chatHistoryListener != null) {
                        chatHistoryListener.onChatHistoryReceived((List<Message>) payload.getData());
                    }
                    break;
                case RECEIVE_MESSAGE:
                    if (messageListener != null) {
                        messageListener.onMessageReceived((Message) payload.getData());
                    }
                    break;
                case CALL_REQUEST:
                    if (callListener != null) {
                        Object[] data = (Object[]) payload.getData();
                        callListener.onIncomingCall((String) data[0], (CallType) data[1]);
                    }
                    break;
                case CALL_ACCEPT:
                    if (callListener != null) {
                        Object[] data = (Object[]) payload.getData();
                        callListener.onCallAccepted((String) data[0], (CallType) data[1]);
                    }
                    break;
                case CALL_REJECT:
                    if (callListener != null) {
                        Object[] data = (Object[]) payload.getData();
                        callListener.onCallRejected((String) data[0], (CallType) data[1]);
                    }
                    break;
                case END_CALL:
                    if (callListener != null) {
                        Object[] data = (Object[]) payload.getData();
                        callListener.onCallEnded((String) data[0]);
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

    public interface StatusUpdateListener {
        void onStatusUpdated(User user);
    }

    public interface CallListener {
        void onIncomingCall(String callerId, CallType type);

        void onCallAccepted(String calleeId, CallType type);

        void onCallRejected(String calleeId, CallType type);

        void onCallEnded(String calleeId);
    }

    public interface ChatHistoryListener {
        void onChatHistoryReceived(List<Message> history);
    }
}
