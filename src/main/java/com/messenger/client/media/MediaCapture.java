package com.messenger.client.media;

import com.messenger.common.CallType;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.Frame;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.util.function.Consumer;
import java.awt.image.BufferedImage;

public class MediaCapture {
    private boolean isCapturing;
    private StreamSender sender;
    private CallType type;

    private TargetDataLine audioLine;
    private OpenCVFrameGrabber grabber;

    public MediaCapture(StreamSender sender, CallType type) {
        this.sender = sender;
        this.type = type;
    }

    public void startCapture(Consumer<byte[]> frameConsumer) {
        if (isCapturing)
            return;
        isCapturing = true;

        new Thread(() -> {
            try {
                if (type == CallType.AUDIO) {
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
                            if (frameConsumer != null)
                                frameConsumer.accept(data);
                            if (sender != null)
                                sender.sendData(data);
                        }
                    }
                } else if (type == CallType.VIDEO) {
                    try {
                        grabber = new OpenCVFrameGrabber(0);
                        grabber.setImageWidth(320);
                        grabber.setImageHeight(240);
                        grabber.start();
                    } catch (org.bytedeco.javacv.FrameGrabber.Exception ex) {
                        System.err.println(
                                "Webcam device not accessible or disabled. Terminating video stream silently.");
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
                                    if (frameConsumer != null)
                                        frameConsumer.accept(data);
                                    if (sender != null)
                                        sender.sendData(data);
                                }
                            }
                            Thread.sleep(30);
                        }
                    }
                }
            } catch (Exception e) {
                if (isCapturing)
                    e.printStackTrace();
            } finally {
                stopCapture();
            }
        }).start();
    }

    public void stopCapture() {
        isCapturing = false;
        try {
            if (audioLine != null) {
                audioLine.stop();
                audioLine.close();
            }
            if (grabber != null) {
                grabber.stop();
                grabber.release();
            }
        } catch (Exception e) {
        }
        if (sender != null) {
            sender.stop();
        }
    }
}
