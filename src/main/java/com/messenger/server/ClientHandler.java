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

    public ClientHandler(Socket socket, MessagingServer server) {
        this.clientSocket = socket;
        this.server = server;
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
        switch (payload.getType()) {
            case AUTH_REQUEST:
                String[] creds = (String[]) payload.getData();
                UserDAO dao = new UserDAO();
                User user = dao.authenticate(creds[0], creds[1]);
                if (user == null) {
                    // For demo purposes, auto-register if doesn't exist
                    user = dao.register(creds[0], creds[1]);
                }

                if (user != null) {
                    associatedUser = user;
                    dao.updateOnlineStatus(user.getId(), true);
                    sendPayload(new NetworkPayload(NetworkPayload.PayloadType.AUTH_RESPONSE, user, "OK"));
                    server.broadcastUserList(); // notify all
                } else {
                    sendPayload(new NetworkPayload(NetworkPayload.PayloadType.AUTH_RESPONSE, null, "ERROR"));
                }
                break;

            case MESSAGE:
                Message msg = (Message) payload.getData();
                server.routeMessage(msg);
                sendPayload(new NetworkPayload(NetworkPayload.PayloadType.MESSAGE_ACK, "SENT"));
                break;

            case LOGOUT_REQUEST:
                disconnect();
                break;

            case CALL_REQUEST:
            case CALL_RESPONSE:
                // The data payload here can be an Object array: [receiverId,
                // signallingData/Port]
                Object[] callData = (Object[]) payload.getData();
                String receiverId = (String) callData[0];
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
        server.removeClient(this);
    }

    public User getAssociatedUser() {
        return associatedUser;
    }
}
