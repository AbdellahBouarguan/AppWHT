package com.messenger.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MessagingServer {
    private int port;
    private List<ClientHandler> activeClients;
    private boolean isRunning;
    private ServerSocket serverSocket;

    public MessagingServer(int port) {
        this.port = port;
        this.activeClients = Collections.synchronizedList(new ArrayList<>());
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
                activeClients.add(handler);
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
            for (ClientHandler handler : activeClients) {
                handler.disconnect();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void broadcastUserList() {
        UserDAO dao = new UserDAO();
        com.messenger.common.NetworkPayload payload = new com.messenger.common.NetworkPayload(
                com.messenger.common.NetworkPayload.PayloadType.USERS_LIST_UPDATE,
                dao.getOnlineUsers());
        for (ClientHandler handler : activeClients) {
            if (handler.getAssociatedUser() != null) {
                handler.sendPayload(payload);
            }
        }
    }

    public void routeMessage(com.messenger.common.Message msg) {
        new MessageDAO().saveMessage(msg);
        for (ClientHandler handler : activeClients) {
            if (handler.getAssociatedUser() != null
                    && handler.getAssociatedUser().getId().equals(msg.getReceiver().getId())) {
                handler.sendPayload(new com.messenger.common.NetworkPayload(
                        com.messenger.common.NetworkPayload.PayloadType.MESSAGE, msg));
                break;
            }
        }
    }

    public void routeSignalingPayload(com.messenger.common.NetworkPayload payload, String receiverId) {
        for (ClientHandler handler : activeClients) {
            if (handler.getAssociatedUser() != null && handler.getAssociatedUser().getId().equals(receiverId)) {
                handler.sendPayload(payload);
                break;
            }
        }
    }

    public void removeClient(ClientHandler handler) {
        activeClients.remove(handler);
        if (handler.getAssociatedUser() != null) {
            new UserDAO().updateOnlineStatus(handler.getAssociatedUser().getId(), false);
            broadcastUserList();
        }
    }

    public static void main(String[] args) {
        // Entry point for running the server independently
        new MessagingServer(1234).start();
    }
}
