package com.messenger.client.media;

import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.Frame;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.util.function.Consumer;
import java.awt.image.BufferedImage;

public class MediaCapture {
    private volatile boolean isCapturing;
    private StreamSender audioSender;
    private StreamSender videoSender;
    private volatile boolean audioEnabled = true;
    private volatile boolean videoEnabled = true;

    private TargetDataLine audioLine;
    private OpenCVFrameGrabber grabber;

    public MediaCapture(StreamSender audioSender, StreamSender videoSender) {
        this.audioSender = audioSender;
        this.videoSender = videoSender;
    }

    public void startCapture(Consumer<byte[]> localVideoConsumer, Consumer<Double> localAudioAmplitudeConsumer) {
        if (isCapturing)
            return;
        isCapturing = true;

        if (audioSender != null) {
            new Thread(() -> captureAudio(localAudioAmplitudeConsumer)).start();
        }
        if (videoSender != null) {
            new Thread(() -> captureVideo(localVideoConsumer)).start();
        }
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

                    if (amplitudeConsumer != null) {
                        amplitudeConsumer.accept(audioEnabled ? calculateAmplitude(data) : 0.0);
                    }
                    if (audioSender != null && audioEnabled) {
                        audioSender.sendData(data);
                    }
                }
            }
        } catch (Exception e) {
            if (isCapturing)
                e.printStackTrace();
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
        } catch (org.bytedeco.javacv.FrameGrabber.Exception ex) {
            System.err.println("Webcam device not accessible or disabled. Terminating video stream silently.");
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

                    if (data.length < 65000) {
                        if (localConsumer != null)
                            localConsumer.accept(videoEnabled ? data : new byte[0]);
                        if (videoSender != null && videoEnabled)
                            videoSender.sendData(data);
                    }
                }
                Thread.sleep(30);
            }
        } catch (Exception e) {
            if (isCapturing)
                e.printStackTrace();
        } finally {
            try {
                if (grabber != null) {
                    grabber.stop();
                    grabber.release();
                }
            } catch (Exception e) {
            }
        }
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
        return Math.min(1.0, rms / 32768.0);
    }

    public void stopCapture() {
        isCapturing = false;
        if (audioSender != null)
            audioSender.stop();
        if (videoSender != null)
            videoSender.stop();
    }

    public void setAudioEnabled(boolean enabled) { this.audioEnabled = enabled; }
    public void setVideoEnabled(boolean enabled) { this.videoEnabled = enabled; }
}
