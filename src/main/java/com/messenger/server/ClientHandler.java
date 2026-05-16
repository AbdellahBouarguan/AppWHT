package com.messenger.server;

import com.messenger.common.Message;
import com.messenger.common.NetworkPayload;
import com.messenger.common.User;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;

public class ClientHandler extends Thread {
    private Socket clientSocket;
    private MessagingServer server;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private User associatedUser;
    private String clientIp;

    public ClientHandler(Socket socket, MessagingServer server) {
        this.clientSocket = socket;
        this.server = server;
        this.clientIp = socket.getInetAddress().getHostAddress();
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            in = new ObjectInputStream(clientSocket.getInputStream());

            while (true) {
                Object obj = in.readObject();
                if (obj instanceof NetworkPayload) {
                    handlePayload((NetworkPayload) obj);
                }
            }
        } catch (EOFException | SocketException e) {
            System.out.println(
                    "Client disconnected: " + (associatedUser != null ? associatedUser.getUsername() : "Unknown"));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            disconnect();
        }
    }

    private void handlePayload(NetworkPayload payload) {
        UserDAO dao = new UserDAO();
        switch (payload.getType()) {
            case REGISTER_REQUEST:
                String[] regCreds = (String[]) payload.getData();
                User newUser = dao.register(regCreds[0], regCreds[1], regCreds[2]);
                if (newUser != null) {
                    associatedUser = newUser;
                    server.registerClient(newUser.getId(), this);
                    sendPayload(new NetworkPayload(NetworkPayload.PayloadType.REGISTER_SUCCESS, newUser, "OK"));
                } else {
                    sendPayload(new NetworkPayload(NetworkPayload.PayloadType.REGISTER_SUCCESS, null, "ERROR"));
                }
                break;

            case LOGIN_REQUEST:
                String[] loginCreds = (String[]) payload.getData();
                User user = dao.authenticate(loginCreds[0], loginCreds[1]);
                if (user != null) {
                    associatedUser = user;
                    server.registerClient(user.getId(), this);
                    sendPayload(new NetworkPayload(NetworkPayload.PayloadType.LOGIN_SUCCESS, user, "OK"));
                } else {
                    sendPayload(new NetworkPayload(NetworkPayload.PayloadType.LOGIN_SUCCESS, null, "ERROR"));
                }
                break;

            case FETCH_CONTACTS_REQUEST:
                if (associatedUser != null) {
                    java.util.List<User> contacts = dao.getContactsForUser(associatedUser.getId());
                    for (User c : contacts) {
                        c.setOnline(server.isClientOnline(c.getId()));
                    }
                    sendPayload(new NetworkPayload(NetworkPayload.PayloadType.CONTACT_LIST_RESPONSE, contacts));
                }
                break;

            case FETCH_CHAT_HISTORY_REQUEST:
                if (associatedUser != null) {
                    String contactId = (String) payload.getData();
                    MessageDAO msgDao = new MessageDAO();
                    java.util.List<Message> history = msgDao.getChatHistory(associatedUser.getId(), contactId);
                    sendPayload(new NetworkPayload(NetworkPayload.PayloadType.FETCH_CHAT_HISTORY_RESPONSE, history));
                }
                break;

            case ADD_CONTACT_REQUEST:
                if (associatedUser != null) {
                    String contactPhoneNumber = (String) payload.getData();
                    dao.addContactByPhone(associatedUser.getId(), contactPhoneNumber);
                    java.util.List<User> newContacts = dao.getContactsForUser(associatedUser.getId());
                    for (User c : newContacts) {
                        c.setOnline(server.isClientOnline(c.getId()));
                    }
                    sendPayload(new NetworkPayload(NetworkPayload.PayloadType.CONTACT_LIST_RESPONSE, newContacts));
                }
                break;

            case SEND_MESSAGE:
                Message msg = (Message) payload.getData();
                server.routeMessage(msg);
                break;

            case MESSAGE_ACK:
                Message ackMsg = (Message) payload.getData();
                server.routeAck(ackMsg);
                break;

            case MESSAGE_READ:
                if (associatedUser != null) {
                    String contactId = (String) payload.getData();
                    new MessageDAO().markAllAsRead(contactId, associatedUser.getId());
                    server.broadcastMessagesRead(contactId, associatedUser.getId());
                }
                break;

            case DELETE_MESSAGE:
                if (associatedUser != null) {
                    Object[] deleteData = (Object[]) payload.getData();
                    String msgUuid = (String) deleteData[0];
                    String recipientId = (String) deleteData[1];
                    new MessageDAO().deleteMessage(msgUuid);
                    server.routeDeleteMessage(msgUuid, recipientId);
                }
                break;

            case MESSAGE_REACTION:
                if (associatedUser != null) {
                    Object[] reactionData = (Object[]) payload.getData();
                    String msgUuid = (String) reactionData[0];
                    String emoji = (String) reactionData[1];
                    String recipientId = (String) reactionData[2];
                    
                    new MessageDAO().addReaction(msgUuid, associatedUser.getId(), emoji);
                    Object[] routedData = new Object[] { msgUuid, associatedUser.getId(), emoji };
                    server.routeSignalingPayload(new NetworkPayload(NetworkPayload.PayloadType.MESSAGE_REACTION, routedData), recipientId);
                }
                break;

            case LOGOUT_REQUEST:
                disconnect();
                break;

            case UPDATE_PROFILE:
                if (associatedUser != null) {
                    byte[] avatarBytes = (byte[]) payload.getData();
                    associatedUser.setAvatarData(avatarBytes);
                    dao.updateUserAvatar(associatedUser.getId(), avatarBytes);
                    server.broadcastStatusUpdate(associatedUser);
                }
                break;

            case TYPING_UPDATE:
                Object[] typingData = (Object[]) payload.getData();
                String targetId = (String) typingData[0];
                boolean isTyping = (Boolean) typingData[1];
                server.routeTypingUpdate(associatedUser.getId(), targetId, isTyping);
                break;

            case CALL_REQUEST:
            case CALL_ACCEPT:
            case CALL_REJECT:
            case END_CALL:
                Object[] callData = (Object[]) payload.getData();
                String receiverId = (String) callData[0];

                // Inject our own IP address so the other peer knows where to stream
                if (payload.getType() == NetworkPayload.PayloadType.CALL_REQUEST ||
                        payload.getType() == NetworkPayload.PayloadType.CALL_ACCEPT) {

                    Object[] newData = new Object[callData.length + 1];
                    System.arraycopy(callData, 0, newData, 0, callData.length);
                    newData[0] = associatedUser; // Replace destination with SENDER so receiver knows who called
                    newData[callData.length] = this.clientIp;
                    payload = new NetworkPayload(payload.getType(), newData);
                } else {
                    // For REJECT and END_CALL, inject sender User too
                    Object[] newData = new Object[callData.length];
                    System.arraycopy(callData, 0, newData, 0, callData.length);
                    newData[0] = associatedUser;
                    payload = new NetworkPayload(payload.getType(), newData);
                }

                server.routeSignalingPayload(payload, receiverId);
                break;

            default:
                break;
        }
    }

    public void sendPayload(NetworkPayload payload) {
        try {
            if (out != null) {
                out.writeObject(payload);
                out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        if (associatedUser != null) {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            associatedUser.setLastSeen(now);
            new UserDAO().updateUserLastSeen(associatedUser.getId(), now);
            associatedUser.setOnline(false);
            
            // 1. Remove from active clients map first so broadcast doesn't include this user
            server.removeClient(this);
            
            // 2. Now broadcast to others
            server.broadcastStatusUpdate(associatedUser);
        } else {
            server.removeClient(this);
        }

        // 3. Finally close resources
        try {
            if (in != null)
                in.close();
            if (out != null)
                out.close();
            if (clientSocket != null)
                clientSocket.close();
        } catch (IOException e) {
            // Ignore
        }
    }

    public User getAssociatedUser() {
        return associatedUser;
    }
}
