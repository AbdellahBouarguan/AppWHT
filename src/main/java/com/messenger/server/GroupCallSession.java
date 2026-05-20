package com.messenger.server;

import com.messenger.common.User;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles active server-routed media relay (SFU) for a specific Group Call.
 * Allocates dynamic UDP ports for audio and video and relays muxed packets.
 */
public class GroupCallSession {
    private final String groupId;
    private final Map<String, ParticipantEndpoint> participants;
    
    private DatagramSocket audioSocket;
    private DatagramSocket videoSocket;
    private volatile boolean isRunning;
    
    private Thread audioRelayThread;
    private Thread videoRelayThread;

    public static class ParticipantEndpoint {
        public final User user;
        public volatile SocketAddress audioDownlink;
        public volatile SocketAddress videoDownlink;
        public volatile boolean isMuted;
        public volatile boolean isCameraOff;

        public ParticipantEndpoint(User user) {
            this.user = user;
        }
    }

    public GroupCallSession(String groupId) {
        this.groupId = groupId;
        this.participants = new ConcurrentHashMap<>();
        this.isRunning = true;
        
        initSockets();
        startRelayThreads();
    }

    private void initSockets() {
        this.audioSocket = bindFreeSocket();
        this.videoSocket = bindFreeSocket();
        System.out.println("Initialized Group Call Session for group: " + groupId 
                + " | Audio UDP Port: " + getAudioPort() 
                + " | Video UDP Port: " + getVideoPort());
    }

    private DatagramSocket bindFreeSocket() {
        Random rand = new Random();
        for (int i = 0; i < 100; i++) {
            int port = 10000 + rand.nextInt(50000);
            try {
                return new DatagramSocket(port);
            } catch (Exception ignored) {}
        }
        // Fallback to ephemeral port assigned by OS
        try {
            return new DatagramSocket(0);
        } catch (Exception e) {
            throw new RuntimeException("Failed to bind any UDP port for media session", e);
        }
    }

    private void startRelayThreads() {
        audioRelayThread = new Thread(() -> relayLoop(audioSocket, true), "GroupAudioRelay-" + groupId);
        videoRelayThread = new Thread(() -> relayLoop(videoSocket, false), "GroupVideoRelay-" + groupId);
        
        audioRelayThread.setDaemon(true);
        videoRelayThread.setDaemon(true);
        
        audioRelayThread.start();
        videoRelayThread.start();
    }

    private void relayLoop(DatagramSocket socket, boolean isAudio) {
        byte[] buffer = new byte[65535]; // Max UDP packet size
        while (isRunning) {
            try {
                DatagramPacket recvPacket = new DatagramPacket(buffer, buffer.length);
                socket.receive(recvPacket);
                
                int len = recvPacket.getLength();
                if (len < 3) continue; // Packet too small
                
                // Parse Magic Header (0xCA11)
                int magic = ((buffer[0] & 0xFF) << 8) | (buffer[1] & 0xFF);
                if (magic != 0xCA11) continue;
                
                int idLen = buffer[2] & 0xFF;
                if (len < 3 + idLen) continue; // Incomplete header
                
                String senderId = new String(buffer, 3, idLen, java.nio.charset.StandardCharsets.UTF_8);
                
                ParticipantEndpoint endpoint = participants.get(senderId);
                if (endpoint == null) {
                    continue; // Unregistered user tried to send media
                }

                // Autodiscover / update client's dynamic UDP downlink endpoint
                SocketAddress remoteAddr = recvPacket.getSocketAddress();
                if (isAudio) {
                    if (endpoint.audioDownlink == null || !endpoint.audioDownlink.equals(remoteAddr)) {
                        endpoint.audioDownlink = remoteAddr;
                        System.out.println("Discovered Audio UDP endpoint for user " + endpoint.user.getUsername() + ": " + remoteAddr);
                    }
                } else {
                    if (endpoint.videoDownlink == null || !endpoint.videoDownlink.equals(remoteAddr)) {
                        endpoint.videoDownlink = remoteAddr;
                        System.out.println("Discovered Video UDP endpoint for user " + endpoint.user.getUsername() + ": " + remoteAddr);
                    }
                }

                // Relay the media packet (preserving header) to all other active call participants
                byte[] relayData = new byte[len];
                System.arraycopy(buffer, 0, relayData, 0, len);
                
                for (ParticipantEndpoint other : participants.values()) {
                    if (other.user.getId().equals(senderId)) {
                        continue; // Do not loop back to sender
                    }
                    
                    SocketAddress dest = isAudio ? other.audioDownlink : other.videoDownlink;
                    if (dest != null) {
                        try {
                            DatagramPacket sendPacket = new DatagramPacket(relayData, len, dest);
                            socket.send(sendPacket);
                        } catch (IOException e) {
                            System.err.println("Failed to relay media packet to " + other.user.getUsername() + ": " + e.getMessage());
                        }
                    }
                }
            } catch (SocketException se) {
                if (!isRunning) break; // Clean socket close on shutdown
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void addParticipant(User user) {
        if (!participants.containsKey(user.getId())) {
            participants.put(user.getId(), new ParticipantEndpoint(user));
            System.out.println("User " + user.getUsername() + " joined call session for group: " + groupId);
        }
    }

    public synchronized void removeParticipant(String userId) {
        ParticipantEndpoint endpoint = participants.remove(userId);
        if (endpoint != null) {
            System.out.println("User " + endpoint.user.getUsername() + " left call session for group: " + groupId);
        }
    }

    public int getAudioPort() {
        return audioSocket != null ? audioSocket.getLocalPort() : -1;
    }

    public int getVideoPort() {
        return videoSocket != null ? videoSocket.getLocalPort() : -1;
    }

    public synchronized List<User> getActiveParticipants() {
        List<User> list = new ArrayList<>();
        for (ParticipantEndpoint ep : participants.values()) {
            list.add(ep.user);
        }
        return list;
    }

    public synchronized int getParticipantCount() {
        return participants.size();
    }

    public synchronized void shutdown() {
        isRunning = false;
        System.out.println("Shutting down Group Call Session for group: " + groupId);
        
        if (audioSocket != null && !audioSocket.isClosed()) {
            audioSocket.close();
        }
        if (videoSocket != null && !videoSocket.isClosed()) {
            videoSocket.close();
        }
        
        participants.clear();
    }

    public String getGroupId() {
        return groupId;
    }
}
