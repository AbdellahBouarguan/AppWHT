package com.messenger.client.media;

import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.Frame;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.util.function.Consumer;
import java.awt.image.BufferedImage;

/**
 * Captures local audio and video feeds, prepends the Selective Forwarding Unit (SFU) 
 * participant header, and streams them to the server's dynamic media ports.
 */
public class GroupMediaCapture {
    private final String userId;
    private final java.util.function.Consumer<byte[]> audioSender;
    private final java.util.function.Consumer<byte[]> videoSender;
    
    private volatile boolean isCapturing;
    private volatile boolean audioEnabled = true;
    private volatile boolean videoEnabled = true;
    
    private TargetDataLine audioLine;
    private OpenCVFrameGrabber grabber;
    private final byte[] userIdBytes;

    public GroupMediaCapture(String userId, java.util.function.Consumer<byte[]> audioSender, java.util.function.Consumer<byte[]> videoSender) {
        this.userId = userId;
        this.audioSender = audioSender;
        this.videoSender = videoSender;
        this.userIdBytes = userId.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    public void startCapture(Consumer<byte[]> localVideoConsumer, Consumer<Double> localAudioAmplitudeConsumer) {
        if (isCapturing) return;
        isCapturing = true;

        if (audioSender != null) {
            new Thread(() -> captureAudio(localAudioAmplitudeConsumer), "GroupAudioCaptureThread").start();
        }
        if (videoSender != null) {
            new Thread(() -> captureVideo(localVideoConsumer), "GroupVideoCaptureThread").start();
        }
        
        // Send initial heartbeat pings to the server dynamic ports so the server 
        // immediately registers our UDP endpoints, even before we start talking or showing video
        sendHeartbeats();
    }

    private void sendHeartbeats() {
        new Thread(() -> {
            byte[] heartbeat = wrapData(new byte[0]);
            for (int i = 0; i < 5; i++) {
                if (!isCapturing) break;
                if (audioSender != null) audioSender.accept(heartbeat);
                if (videoSender != null) videoSender.accept(heartbeat);
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ignored) {}
            }
        }, "UDPHeartbeatThread").start();
    }

    private void captureAudio(Consumer<Double> amplitudeConsumer) {
        try {
            AudioFormat format = new AudioFormat(8000.0f, 16, 1, true, true);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            audioLine = (TargetDataLine) AudioSystem.getLine(info);
            audioLine.open(format);
            audioLine.start();

            byte[] buffer = new byte[1024];
            while (isCapturing) {
                int bytesRead = audioLine.read(buffer, 0, buffer.length);
                if (bytesRead > 0) {
                    byte[] data = new byte[bytesRead];
                    System.arraycopy(buffer, 0, data, 0, bytesRead);

                    double amp = audioEnabled ? calculateAmplitude(data) : 0.0;
                    if (amplitudeConsumer != null) {
                        amplitudeConsumer.accept(amp);
                    }
                    if (audioSender != null && audioEnabled) {
                        byte[] packetData = wrapData(data);
                        audioSender.accept(packetData);
                    }
                }
            }
        } catch (Exception e) {
            if (isCapturing) e.printStackTrace();
        } finally {
            if (audioLine != null) {
                audioLine.stop();
                audioLine.close();
            }
        }
    }

    private void captureVideo(Consumer<byte[]> localConsumer) {
        try {
            grabber = new OpenCVFrameGrabber(0);
            grabber.setImageWidth(320);
            grabber.setImageHeight(240);
            grabber.start();
        } catch (Exception ex) {
            System.err.println("Webcam device not accessible or disabled. Group call proceeding in audio-only mode.");
            if (localConsumer != null) {
                localConsumer.accept(new byte[0]); // signal webcam error / fallback
            }
            return;
        }

        try (Java2DFrameConverter converter = new Java2DFrameConverter()) {
            while (isCapturing) {
                Frame frame = grabber.grab();
                if (frame != null && frame.image != null) {
                    BufferedImage image = converter.getBufferedImage(frame);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(image, "jpg", baos);
                    byte[] data = baos.toByteArray();

                    // Max UDP safe packet size is ~65,000 bytes
                    if (data.length < 60000) {
                        if (localConsumer != null) {
                            localConsumer.accept(videoEnabled ? data : new byte[0]);
                        }
                        if (videoSender != null && videoEnabled) {
                            byte[] packetData = wrapData(data);
                            videoSender.accept(packetData);
                        }
                    }
                }
                Thread.sleep(30);
            }
        } catch (Exception e) {
            if (isCapturing) e.printStackTrace();
        } finally {
            try {
                if (grabber != null) {
                    grabber.stop();
                    grabber.release();
                }
            } catch (Exception ignored) {}
        }
    }

    private byte[] wrapData(byte[] payload) {
        int headerLen = 2 + 1 + userIdBytes.length;
        byte[] packetData = new byte[headerLen + payload.length];
        
        // Magic bytes (0xCA11)
        packetData[0] = (byte) ((0xCA11 >> 8) & 0xFF);
        packetData[1] = (byte) (0xCA11 & 0xFF);
        
        // Sender ID string length
        packetData[2] = (byte) (userIdBytes.length & 0xFF);
        
        // Sender ID
        System.arraycopy(userIdBytes, 0, packetData, 3, userIdBytes.length);
        
        // Actual media data
        System.arraycopy(payload, 0, packetData, headerLen, payload.length);
        
        return packetData;
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

    public void stopCapture() {
        isCapturing = false;
    }

    public void setAudioEnabled(boolean enabled) {
        this.audioEnabled = enabled;
    }

    public void setVideoEnabled(boolean enabled) {
        this.videoEnabled = enabled;
    }
}
