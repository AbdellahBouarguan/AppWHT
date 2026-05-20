package com.messenger.client;

import com.messenger.common.NetworkPayload;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class NetworkClient {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String serverIp;
    private int port;
    private ChatManager chatManager;

    public NetworkClient(String serverIp, int port, ChatManager chatManager) {
        this.serverIp = serverIp;
        this.port = port;
        this.chatManager = chatManager;
    }

    public boolean connect() {
        try {
            socket = new Socket(serverIp, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            // Start listening thread
            Thread listenThread = new Thread(this::listen);
            listenThread.setDaemon(true);
            listenThread.start();
            return true;
        } catch (IOException e) {
            System.err.println("Failed to connect to server at " + serverIp + ":" + port);
            return false;
        }
    }

    public void login(String username, String password) {
        sendData(new NetworkPayload(NetworkPayload.PayloadType.LOGIN_REQUEST, new String[] { username, password }));
    }

    public void register(String username, String password, String phoneNumber) {
        sendData(new NetworkPayload(NetworkPayload.PayloadType.REGISTER_REQUEST,
                new String[] { username, password, phoneNumber }));
    }

    public void fetchContacts() {
        sendData(new NetworkPayload(NetworkPayload.PayloadType.FETCH_CONTACTS_REQUEST, null));
    }

    public void addContact(String contactId) {
        sendData(new NetworkPayload(NetworkPayload.PayloadType.ADD_CONTACT_REQUEST, contactId));
    }

    public void createGroup(String name, String description, java.util.List<String> memberIds) {
        sendData(new NetworkPayload(NetworkPayload.PayloadType.CREATE_GROUP_REQUEST,
                new Object[] { name, description, memberIds }));
    }

    public void fetchGroups() {
        sendData(new NetworkPayload(NetworkPayload.PayloadType.FETCH_GROUPS_REQUEST, null));
    }

    public void sendData(NetworkPayload payload) {
        try {
            if (out != null) {
                out.reset();
                out.writeObject(payload);
                out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void listen() {
        try {
            while (true) {
                Object obj = in.readObject();
                if (obj instanceof NetworkPayload) {
                    NetworkPayload payload = (NetworkPayload) obj;
                    chatManager.handlePayload(payload);
                }
            }
        } catch (Exception e) {
            System.err.println("Disconnected from server.");
        }
    }

    public void disconnect() {
        try {
            if (socket != null)
                socket.close();
            if (in != null)
                in.close();
            if (out != null)
                out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getServerIp() {
        return serverIp;
    }
}
