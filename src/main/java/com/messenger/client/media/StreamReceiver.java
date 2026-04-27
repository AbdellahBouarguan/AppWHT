package com.messenger.client.media;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.function.Consumer;

public class StreamReceiver {
    private DatagramSocket socket;
    private boolean isListening;

    public StreamReceiver(int listenPort) {
        try {
            this.socket = new DatagramSocket(listenPort);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startListening(Consumer<byte[]> dataConsumer) {
        isListening = true;
        new Thread(() -> {
            try {
                byte[] buffer = new byte[65535]; // Max UDP packet size
                while (isListening) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    // Copy exact data received
                    byte[] data = new byte[packet.getLength()];
                    System.arraycopy(packet.getData(), 0, data, 0, packet.getLength());

                    if (dataConsumer != null) {
                        dataConsumer.accept(data);
                    }
                }
            } catch (Exception e) {
                if (isListening)
                    e.printStackTrace();
            }
        }).start();
    }

    public void stop() {
        isListening = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}
