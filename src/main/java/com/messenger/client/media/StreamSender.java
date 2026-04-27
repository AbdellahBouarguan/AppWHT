package com.messenger.client.media;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class StreamSender {
    private DatagramSocket socket;
    private InetAddress targetAddress;
    private int targetPort;

    public StreamSender(String targetIp, int targetPort) {
        try {
            this.socket = new DatagramSocket();
            this.targetAddress = InetAddress.getByName(targetIp);
            this.targetPort = targetPort;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendData(byte[] data) {
        try {
            DatagramPacket packet = new DatagramPacket(data, data.length, targetAddress, targetPort);
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}
