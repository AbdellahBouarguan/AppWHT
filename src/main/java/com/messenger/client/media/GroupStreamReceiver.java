package com.messenger.client.media;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

/**
 * Manages the single local bidirectional UDP sockets for audio and video.
 * Receives the multiplexed streams, parses the headers, opens dynamic Audio Lines for 
 * each sender (OS mixers blend them), and dispatches video frames to the UI grid.
 */
public class GroupStreamReceiver {
    private DatagramSocket audioSocket;
    private DatagramSocket videoSocket;
    
    private InetAddress serverAddress;
    private int serverAudioPort;
    private int serverVideoPort;
    
    private volatile boolean isListening;
    private final Map<String, ParticipantAudioPlayer> audioPlayers;
    private final AudioFormat audioFormat;
    
    private BiConsumer<String, byte[]> videoFrameConsumer;
    private BiConsumer<String, Double> audioAmplitudeConsumer;

    public GroupStreamReceiver() {
        this.audioPlayers = new ConcurrentHashMap<>();
        this.audioFormat = new AudioFormat(8000.0f, 16, 1, true, true);
        
        try {
            // Bind to ephemeral UDP ports assigned by the OS
            this.audioSocket = new DatagramSocket();
            this.videoSocket = new DatagramSocket();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void start(String serverIp, int serverAudioPort, int serverVideoPort) {
        if (isListening) return;
        this.serverAudioPort = serverAudioPort;
        this.serverVideoPort = serverVideoPort;
        
        try {
            this.serverAddress = InetAddress.getByName(serverIp);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        isListening = true;
        
        new Thread(() -> listenLoop(audioSocket, true), "GroupAudioReceiverThread").start();
        new Thread(() -> listenLoop(videoSocket, false), "GroupVideoReceiverThread").start();
    }

    private void listenLoop(DatagramSocket socket, boolean isAudio) {
        byte[] buffer = new byte[65535];
        while (isListening) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                
                int len = packet.getLength();
                if (len < 3) continue;
                
                // Parse Magic Header (0xCA11)
                int magic = ((buffer[0] & 0xFF) << 8) | (buffer[1] & 0xFF);
                if (magic != 0xCA11) continue;
                
                int idLen = buffer[2] & 0xFF;
                if (len < 3 + idLen) continue;
                
                String senderId = new String(buffer, 3, idLen, java.nio.charset.StandardCharsets.UTF_8);
                
                int payloadOffset = 3 + idLen;
                int payloadLen = len - payloadOffset;
                
                byte[] payload = new byte[payloadLen];
                System.arraycopy(buffer, payloadOffset, payload, 0, payloadLen);
                
                if (isAudio) {
                    playAudio(senderId, payload);
                } else {
                    dispatchVideoFrame(senderId, payload);
                }
            } catch (IOException e) {
                if (!isListening) break; // Clean close on stop
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void playAudio(String senderId, byte[] rawAudio) {
        if (rawAudio.length == 0) return; // ignore keep-alives/heartbeats
        
        ParticipantAudioPlayer player = audioPlayers.get(senderId);
        if (player == null) {
            synchronized (audioPlayers) {
                player = audioPlayers.get(senderId);
                if (player == null) {
                    try {
                        player = new ParticipantAudioPlayer(senderId, audioFormat);
                        audioPlayers.put(senderId, player);
                        System.out.println("Started dynamic hardware audio mixing line for call participant: " + senderId);
                    } catch (Exception e) {
                        System.err.println("Failed to start audio player for " + senderId + ": " + e.getMessage());
                        return;
                    }
                }
            }
        }
        player.queueAudio(rawAudio);
    }

    private void dispatchVideoFrame(String senderId, byte[] frameData) {
        if (videoFrameConsumer != null) {
            videoFrameConsumer.accept(senderId, frameData);
        }
    }

    private double calculateAmplitude(byte[] data) {
        long sum = 0;
        int count = data.length / 2;
        if (count == 0) return 0;
        for (int i = 0; i < data.length - 1; i += 2) {
            short sample = (short) ((data[i] & 0xFF) | (data[i + 1] << 8));
            sum += (long) sample * sample;
        }
        double rms = Math.sqrt((double) sum / count);
        return Math.min(1.0, rms / 32768.0);
    }

    public void sendAudio(byte[] wrappedData) {
        if (audioSocket != null && serverAddress != null) {
            try {
                DatagramPacket packet = new DatagramPacket(wrappedData, wrappedData.length, serverAddress, serverAudioPort);
                audioSocket.send(packet);
            } catch (IOException e) {
                // Ignore send errors during transitions
            }
        }
    }

    public void sendVideo(byte[] wrappedData) {
        if (videoSocket != null && serverAddress != null) {
            try {
                DatagramPacket packet = new DatagramPacket(wrappedData, wrappedData.length, serverAddress, serverVideoPort);
                videoSocket.send(packet);
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    public void setVideoFrameConsumer(BiConsumer<String, byte[]> consumer) {
        this.videoFrameConsumer = consumer;
    }

    public void setAudioAmplitudeConsumer(BiConsumer<String, Double> consumer) {
        this.audioAmplitudeConsumer = consumer;
    }

    public void stop() {
        isListening = false;
        
        // Close network sockets
        if (audioSocket != null && !audioSocket.isClosed()) {
            audioSocket.close();
        }
        if (videoSocket != null && !videoSocket.isClosed()) {
            videoSocket.close();
        }
        
        // Clean up audio players
        synchronized (audioPlayers) {
            for (ParticipantAudioPlayer player : audioPlayers.values()) {
                player.stop();
            }
            audioPlayers.clear();
        }
        System.out.println("Cleaned up group calling receiver and all client audio lines.");
    }

    private class ParticipantAudioPlayer {
        private final String senderId;
        private final java.util.concurrent.LinkedBlockingQueue<byte[]> queue;
        private final SourceDataLine line;
        private volatile boolean running;
        private final Thread thread;

        public ParticipantAudioPlayer(String senderId, AudioFormat format) throws Exception {
            this.senderId = senderId;
            this.queue = new java.util.concurrent.LinkedBlockingQueue<>(50); // limit queue size
            
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            this.line = (SourceDataLine) AudioSystem.getLine(info);
            this.line.open(format, 4096); // explicit small buffer to reduce latency
            this.line.start();
            
            this.running = true;
            this.thread = new Thread(this::playLoop, "AudioPlayer-" + senderId);
            this.thread.setDaemon(true);
            this.thread.start();
        }

        public void queueAudio(byte[] data) {
            if (!running) return;
            // Drop oldest packets if the queue grows to prevent accumulation of lag
            while (queue.size() >= 15) {
                queue.poll();
            }
            queue.offer(data);
        }

        private void playLoop() {
            while (running && isListening) {
                try {
                    byte[] data = queue.poll(100, java.util.concurrent.TimeUnit.MILLISECONDS);
                    if (data != null && running && isListening) {
                        line.write(data, 0, data.length);
                        if (audioAmplitudeConsumer != null) {
                            double amp = calculateAmplitude(data);
                            audioAmplitudeConsumer.accept(senderId, amp);
                        }
                    }
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    // Ignore errors during thread shutdown or device issues
                }
            }
        }

        public void stop() {
            running = false;
            thread.interrupt();
            try {
                line.stop();
                line.close();
            } catch (Exception ignored) {}
        }
    }
}
