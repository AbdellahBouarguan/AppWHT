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
    private TypingListener typingListener;
    private StatusUpdateListener statusUpdateListener;
    private ChatHistoryListener chatHistoryListener;
    private Object[] latestCallData;

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

    public void sendDirectMessage(com.messenger.common.Message msg) {
        if (networkClient != null) {
            networkClient.sendData(new NetworkPayload(NetworkPayload.PayloadType.SEND_MESSAGE, msg));
        }
    }

    public com.messenger.common.Message sendMessage(User receiver, String text, com.messenger.common.Message parentMsg) {
        com.messenger.common.Message msg = new com.messenger.common.Message(currentUser, receiver, text);
        if (parentMsg != null) {
            msg.setParentMessageId(parentMsg.getId());
            msg.setParentMessageContent(parentMsg.getContent());
        }

        networkClient.sendData(new NetworkPayload(NetworkPayload.PayloadType.SEND_MESSAGE, msg));
        return msg;
    }

    public com.messenger.common.Message sendFileMessage(User receiver, String text, java.io.File file, com.messenger.common.Message parentMsg) {
        try {
            byte[] fileData = java.nio.file.Files.readAllBytes(file.toPath());
            return sendMessage(receiver.getId(), text, file.getName(), fileData, parentMsg);
        } catch (java.io.IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public com.messenger.common.Message sendMessage(String receiverId, String text, String fileName, byte[] fileData, com.messenger.common.Message parentMsg) {
        User receiver = new User(receiverId, "");
        com.messenger.common.Message msg = new com.messenger.common.Message(currentUser, receiver, text);
        if (parentMsg != null) {
            msg.setParentMessageId(parentMsg.getId());
            msg.setParentMessageContent(parentMsg.getContent());
        }
        if (fileName != null) msg.setFileName(fileName);
        if (fileData != null) msg.setFileData(fileData);
        
        networkClient.sendData(new NetworkPayload(NetworkPayload.PayloadType.SEND_MESSAGE, msg));
        return msg;
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

    public void updateProfilePicture(java.io.File file) {
        try {
            byte[] avatarData = java.nio.file.Files.readAllBytes(file.toPath());
            if (currentUser != null) {
                currentUser.setAvatarData(avatarData);
                networkClient.sendData(new NetworkPayload(NetworkPayload.PayloadType.UPDATE_PROFILE, avatarData));
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
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

    public interface MessageAckListener {
        void onMessageAck(Message msg);
    }

    public interface MessagesReadListener {
        void onMessagesRead(String readerId);
    }

    public interface MessageDeletedListener {
        void onMessageDeleted(String messageUuid);
    }

    public interface MessageReactionListener {
        void onMessageReaction(String messageUuid, String userId, String emoji);
    }

    private MessageAckListener messageAckListener;
    private MessagesReadListener messagesReadListener;
    private MessageDeletedListener messageDeletedListener;
    private MessageReactionListener messageReactionListener;

    public void setCallListener(CallListener callListener) {
        this.callListener = callListener;
    }

    public void setTypingListener(TypingListener typingListener) {
        this.typingListener = typingListener;
    }

    public void setChatHistoryListener(ChatHistoryListener listener) {
        this.chatHistoryListener = listener;
    }

    public void setMessageAckListener(MessageAckListener listener) {
        this.messageAckListener = listener;
    }

    public void setMessagesReadListener(MessagesReadListener listener) {
        this.messagesReadListener = listener;
    }

    public void setMessageDeletedListener(MessageDeletedListener listener) {
        this.messageDeletedListener = listener;
    }

    public void setMessageReactionListener(MessageReactionListener listener) {
        this.messageReactionListener = listener;
    }

    public void addReaction(String uuid, String emoji, String recipientId) {
        if (networkClient != null) {
            Object[] reactionData = new Object[] { uuid, emoji, recipientId };
            networkClient.sendData(new NetworkPayload(NetworkPayload.PayloadType.MESSAGE_REACTION, reactionData));
        }
    }

    public void deleteMessage(String uuid, String recipientId) {
        if (networkClient != null) {
            Object[] deleteData = new Object[] { uuid, recipientId };
            networkClient.sendData(new NetworkPayload(NetworkPayload.PayloadType.DELETE_MESSAGE, deleteData));
        }
    }

    public void sendReadReceipt(String contactId) {
        if (networkClient != null) {
            networkClient.sendData(new NetworkPayload(NetworkPayload.PayloadType.MESSAGE_READ, contactId));
        }
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
                    Message recvMsg = (Message) payload.getData();
                    recvMsg.setStatus(Message.MessageStatus.DELIVERED_TO_CLIENT);
                    networkClient.sendData(new NetworkPayload(NetworkPayload.PayloadType.MESSAGE_ACK, recvMsg));
                    
                    if (messageListener != null) {
                        messageListener.onMessageReceived(recvMsg);
                    }
                    break;
                case MESSAGE_ACK:
                    if (messageAckListener != null) {
                        messageAckListener.onMessageAck((Message) payload.getData());
                    }
                    break;
                case MESSAGE_READ:
                    if (messagesReadListener != null) {
                        messagesReadListener.onMessagesRead((String) payload.getData());
                    }
                    break;
                case DELETE_MESSAGE:
                    if (messageDeletedListener != null) {
                        messageDeletedListener.onMessageDeleted((String) payload.getData());
                    }
                    break;
                case MESSAGE_REACTION:
                    if (messageReactionListener != null) {
                        Object[] reactionData = (Object[]) payload.getData();
                        messageReactionListener.onMessageReaction((String) reactionData[0], (String) reactionData[1], (String) reactionData[2]);
                    }
                    break;
                case CALL_REQUEST:
                case CALL_ACCEPT:
                case CALL_REJECT:
                case END_CALL:
                    if (callListener != null) {
                        Object[] callData = (Object[]) payload.getData();
                        this.latestCallData = callData;
                        User peer = (User) callData[0];
                        com.messenger.common.CallType callType = (com.messenger.common.CallType) callData[1];

                        switch (payload.getType()) {
                            case CALL_REQUEST:
                                callListener.onIncomingCall(peer, callType);
                                break;
                            case CALL_ACCEPT:
                                callListener.onCallAccepted(peer, callType);
                                break;
                            case CALL_REJECT:
                                callListener.onCallRejected(peer, callType);
                                break;
                            case END_CALL:
                                callListener.onCallEnded(peer);
                                break;
                        }
                    }
                    break;
                case TYPING_UPDATE:
                    if (typingListener != null) {
                        Object[] typingData = (Object[]) payload.getData();
                        String senderId = (String) typingData[0];
                        boolean isTyping = (Boolean) typingData[1];
                        typingListener.onTypingUpdate(senderId, isTyping);
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
        void onIncomingCall(User caller, CallType type);

        void onCallAccepted(User callee, CallType type);

        void onCallRejected(User callee, CallType type);

        void onCallEnded(User callee);
    }

    public interface TypingListener {
        void onTypingUpdate(String senderId, boolean isTyping);
    }

    public void sendTypingUpdate(String receiverId, boolean isTyping) {
        if (networkClient != null) {
            Object[] data = {receiverId, isTyping};
            networkClient.sendData(new NetworkPayload(NetworkPayload.PayloadType.TYPING_UPDATE, data));
        }
    }

    public Object[] getLatestCallData() {
        return latestCallData;
    }

    public interface ChatHistoryListener {
        void onChatHistoryReceived(List<Message> history);
    }
}
