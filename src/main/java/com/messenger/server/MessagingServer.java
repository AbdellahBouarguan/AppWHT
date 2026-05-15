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
    private boolean isRunning;
    private ServerSocket serverSocket;

    public MessagingServer(int port) {
        this.port = port;
        this.activeClients = new java.util.concurrent.ConcurrentHashMap<>();
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
        broadcastStatusUpdate(userId, true);

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

    public void broadcastStatusUpdate(String userId, boolean isOnline) {
        com.messenger.common.User statusUser = new com.messenger.common.User(userId, "");
        statusUser.setOnline(isOnline);

        com.messenger.common.NetworkPayload payload = new com.messenger.common.NetworkPayload(
                com.messenger.common.NetworkPayload.PayloadType.STATUS_UPDATE,
                statusUser);

        for (ClientHandler handler : activeClients.values()) {
            handler.sendPayload(payload);
        }
    }

    public boolean isClientOnline(String userId) {
        return activeClients.containsKey(userId);
    }

    public void routeMessage(com.messenger.common.Message msg) {
        MessageDAO mDao = new MessageDAO();
        ClientHandler senderHandler = activeClients.get(msg.getSender().getId());
        ClientHandler receiverHandler = activeClients.get(msg.getReceiver().getId());

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

    public void routeAck(com.messenger.common.Message msg) {
        MessageDAO mDao = new MessageDAO();
        mDao.updateMessageStatus(msg.getId(), msg.getStatus());
        
        ClientHandler senderHandler = activeClients.get(msg.getSender().getId());
        if (senderHandler != null) {
            senderHandler.sendPayload(new com.messenger.common.NetworkPayload(
                    com.messenger.common.NetworkPayload.PayloadType.MESSAGE_ACK, msg));
        }
    }

    public void routeTypingUpdate(String senderId, String receiverId, boolean isTyping) {
        ClientHandler receiverHandler = activeClients.get(receiverId);
        if (receiverHandler != null) {
            Object[] data = {senderId, isTyping};
            receiverHandler.sendPayload(new com.messenger.common.NetworkPayload(
                    com.messenger.common.NetworkPayload.PayloadType.TYPING_UPDATE, data));
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
            broadcastStatusUpdate(userId, false);
        }
    }

    public static void main(String[] args) {
        // Entry point for running the server independently
        new MessagingServer(1234).start();
    }
}
