package com.messenger.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MessagingServer {
    private int port;
    private java.util.Map<String, ClientHandler> activeClients;
    private java.util.Map<String, GroupCallSession> activeGroupCalls;
    private boolean isRunning;
    private ServerSocket serverSocket;

    public MessagingServer(int port) {
        this.port = port;
        this.activeClients = new java.util.concurrent.ConcurrentHashMap<>();
        this.activeGroupCalls = new java.util.concurrent.ConcurrentHashMap<>();
    }

    public void start() {
        DatabaseManager.initializeDatabase();
        isRunning = true;
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("MessagingServer started on port " + port);

            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());
                ClientHandler handler = new ClientHandler(clientSocket, this);
                handler.start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    public void stop() {
        isRunning = false;
        try {
            if (serverSocket != null)
                serverSocket.close();
            for (ClientHandler handler : activeClients.values()) {
                handler.disconnect();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void registerClient(String userId, ClientHandler handler) {
        activeClients.put(userId, handler);
        if (handler.getAssociatedUser() != null) {
            handler.getAssociatedUser().setOnline(true);
            broadcastStatusUpdate(handler.getAssociatedUser());
        }

        // Deliver offline messages
        MessageDAO mDao = new MessageDAO();
        java.util.List<com.messenger.common.Message> offlineMsgs = mDao.getUndeliveredMessages(userId);
        for (com.messenger.common.Message msg : offlineMsgs) {
            handler.sendPayload(new com.messenger.common.NetworkPayload(
                    com.messenger.common.NetworkPayload.PayloadType.RECEIVE_MESSAGE, msg));
        }
        if (!offlineMsgs.isEmpty()) {
            mDao.markAllAsDelivered(userId);
        }
    }

    public void broadcastStatusUpdate(com.messenger.common.User statusUser) {
        com.messenger.common.NetworkPayload payload = new com.messenger.common.NetworkPayload(
                com.messenger.common.NetworkPayload.PayloadType.STATUS_UPDATE,
                statusUser);

        // Copy to avoid ConcurrentModificationException
        java.util.List<ClientHandler> handlers = new java.util.ArrayList<>(activeClients.values());
        for (ClientHandler handler : handlers) {
            handler.sendPayload(payload);
        }
    }

    public boolean isClientOnline(String userId) {
        return activeClients.containsKey(userId);
    }

    public void routeMessage(com.messenger.common.Message msg) {
        MessageDAO mDao = new MessageDAO();
        UserDAO uDao = new UserDAO();
        ClientHandler senderHandler = activeClients.get(msg.getSender().getId());
        ClientHandler receiverHandler = activeClients.get(msg.getReceiver().getId());

        String status = uDao.getContactStatus(msg.getReceiver().getId(), msg.getSender().getId());
        if ("BLOCKED".equals(status)) {
            msg.setStatus(com.messenger.common.Message.MessageStatus.SENT_TO_SERVER);
            if (senderHandler != null) {
                senderHandler.sendPayload(new com.messenger.common.NetworkPayload(
                        com.messenger.common.NetworkPayload.PayloadType.MESSAGE_ACK, msg));
            }
            return;
        }

        if (status == null) {
            uDao.updateContactStatus(msg.getReceiver().getId(), msg.getSender().getId(), "PENDING");
            if (receiverHandler != null) {
                java.util.List<com.messenger.common.User> newContacts = uDao.getContactsForUser(msg.getReceiver().getId());
                for (com.messenger.common.User c : newContacts) {
                    c.setOnline(isClientOnline(c.getId()));
                }
                receiverHandler.sendPayload(new com.messenger.common.NetworkPayload(
                        com.messenger.common.NetworkPayload.PayloadType.CONTACT_LIST_RESPONSE, newContacts));
            }
        }

        msg.setStatus(com.messenger.common.Message.MessageStatus.SENT_TO_SERVER);
        mDao.saveMessage(msg); 
        
        // Notify sender that server received it
        if (senderHandler != null) {
            senderHandler.sendPayload(new com.messenger.common.NetworkPayload(
                    com.messenger.common.NetworkPayload.PayloadType.MESSAGE_ACK, msg));
        }

        if (receiverHandler != null) {
            receiverHandler.sendPayload(new com.messenger.common.NetworkPayload(
                    com.messenger.common.NetworkPayload.PayloadType.RECEIVE_MESSAGE, msg));
        }
    }

    public void routeGroupMessage(com.messenger.common.Message msg) {
        MessageDAO mDao = new MessageDAO();
        GroupDAO gDao = new GroupDAO();

        msg.setStatus(com.messenger.common.Message.MessageStatus.SENT_TO_SERVER);
        mDao.saveMessage(msg);

        // Fetch all group member IDs
        java.util.List<String> memberIds = gDao.getGroupMemberIds(msg.getGroupId());
        
        // Make thread-safe copy of active clients to avoid ConcurrentModificationException
        java.util.Map<String, ClientHandler> clientsCopy = new java.util.concurrent.ConcurrentHashMap<>(activeClients);

        // Notify sender that server received it
        ClientHandler senderHandler = clientsCopy.get(msg.getSender().getId());
        if (senderHandler != null) {
            senderHandler.sendPayload(new com.messenger.common.NetworkPayload(
                    com.messenger.common.NetworkPayload.PayloadType.MESSAGE_ACK, msg));
        }

        // Iterate through all group members
        for (String memberId : memberIds) {
            if (memberId.equals(msg.getSender().getId())) {
                continue; // Skip the sender as we already sent MESSAGE_ACK
            }

            ClientHandler handler = clientsCopy.get(memberId);
            if (handler != null) {
                // Member is online: route real-time message and save receipt as DELIVERED (2)
                mDao.saveGroupMessageReceipt(msg.getId(), memberId, 2);
                handler.sendPayload(new com.messenger.common.NetworkPayload(
                        com.messenger.common.NetworkPayload.PayloadType.GROUP_MESSAGE_RECEIVE, msg));
            } else {
                // Member is offline: save receipt as SENT_TO_SERVER (1) for offline delivery
                mDao.saveGroupMessageReceipt(msg.getId(), memberId, 1);
            }
        }
    }

    public void routeAck(com.messenger.common.Message msg) {
        MessageDAO mDao = new MessageDAO();
        mDao.updateMessageStatus(msg.getId(), msg.getStatus());
        
        ClientHandler senderHandler = activeClients.get(msg.getSender().getId());
        if (senderHandler != null) {
            senderHandler.sendPayload(new com.messenger.common.NetworkPayload(
                    com.messenger.common.NetworkPayload.PayloadType.MESSAGE_ACK, msg));
        }
    }

    public void broadcastMessagesRead(String originalSenderId, String readerId) {
        ClientHandler senderHandler = activeClients.get(originalSenderId);
        if (senderHandler != null) {
            senderHandler.sendPayload(new com.messenger.common.NetworkPayload(
                    com.messenger.common.NetworkPayload.PayloadType.MESSAGE_READ, readerId));
        }
    }

    public void routeDeleteMessage(String messageUuid, String recipientId) {
        ClientHandler handler = activeClients.get(recipientId);
        if (handler != null) {
            handler.sendPayload(new com.messenger.common.NetworkPayload(
                    com.messenger.common.NetworkPayload.PayloadType.DELETE_MESSAGE, messageUuid));
        }
    }

    public void routeTypingUpdate(String senderId, String targetId, boolean isTyping) {
        com.messenger.server.GroupDAO gDao = new com.messenger.server.GroupDAO();
        if (gDao.isGroup(targetId)) {
            java.util.List<String> memberIds = gDao.getGroupMemberIds(targetId);
            java.util.Map<String, ClientHandler> clientsCopy = new java.util.concurrent.ConcurrentHashMap<>(activeClients);
            Object[] data = {senderId, targetId, isTyping};
            com.messenger.common.NetworkPayload payload = new com.messenger.common.NetworkPayload(
                    com.messenger.common.NetworkPayload.PayloadType.TYPING_UPDATE, data);
            for (String memberId : memberIds) {
                if (memberId.equals(senderId)) continue;
                ClientHandler h = clientsCopy.get(memberId);
                if (h != null) h.sendPayload(payload);
            }
        } else {
            ClientHandler receiverHandler = activeClients.get(targetId);
            if (receiverHandler != null) {
                Object[] data = {senderId, targetId, isTyping};
                receiverHandler.sendPayload(new com.messenger.common.NetworkPayload(
                        com.messenger.common.NetworkPayload.PayloadType.TYPING_UPDATE, data));
            }
        }
    }

    public void routeSignalingPayload(com.messenger.common.NetworkPayload payload, String receiverId) {
        ClientHandler handler = activeClients.get(receiverId);
        if (handler != null) {
            handler.sendPayload(payload);
        }
    }

    public void removeClient(ClientHandler handler) {
        if (handler.getAssociatedUser() != null) {
            String userId = handler.getAssociatedUser().getId();
            activeClients.remove(userId);
            // broadcastStatusUpdate is already called in ClientHandler.disconnect()
        }
    }

    public java.util.Map<String, ClientHandler> getActiveClients() {
        return activeClients;
    }

    public synchronized GroupCallSession getOrCreateGroupCall(String groupId) {
        GroupCallSession session = activeGroupCalls.get(groupId);
        if (session == null) {
            session = new GroupCallSession(groupId);
            activeGroupCalls.put(groupId, session);
        }
        return session;
    }

    public synchronized void removeGroupCallParticipant(String groupId, String userId) {
        GroupCallSession session = activeGroupCalls.get(groupId);
        if (session != null) {
            session.removeParticipant(userId);
            if (session.getParticipantCount() == 0) {
                session.shutdown();
                activeGroupCalls.remove(groupId);
                System.out.println("Cleaned up empty group call for group: " + groupId);
            }
        }
    }

    public java.util.Map<String, GroupCallSession> getActiveGroupCalls() {
        return activeGroupCalls;
    }

    public static void main(String[] args) {
        // Entry point for running the server independently
        new MessagingServer(1234).start();
    }
}
