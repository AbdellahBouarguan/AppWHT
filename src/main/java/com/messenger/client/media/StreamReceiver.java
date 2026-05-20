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
        startListening(dataConsumer, null);
    }

    public void startListening(Consumer<byte[]> dataConsumer, Consumer<Double> amplitudeConsumer) {
        if (socket == null) {
            System.err.println("Cannot listen: Socket binding failed (port already in use?)");
            return;
        }
        isListening = true;
        new Thread(() -> {
            try {
                byte[] buffer = new byte[65535]; // Max UDP packet size
                while (isListening) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    byte[] data = new byte[packet.getLength()];
                    System.arraycopy(packet.getData(), 0, data, 0, packet.getLength());

                    if (amplitudeConsumer != null) {
                        double amplitude = calculateAmplitude(data);
                        amplitudeConsumer.accept(amplitude);
                    }

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

    private double calculateAmplitude(byte[] data) {
        long sum = 0;
        int count = data.length / 2;
        if (count == 0)
            return 0;
        for (int i = 0; i < data.length - 1; i += 2) {
            short sample = (short) ((data[i] & 0xFF) | (data[i + 1] << 8));
            sum += (long) sample * sample;
        }
        double rms = Math.sqrt((double) sum / count);
        return Math.min(1.0, rms / 32768.0); // Normalize to 0.0 - 1.0 (16-bit max is 32768)
    }

    public void stop() {
        isListening = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}
