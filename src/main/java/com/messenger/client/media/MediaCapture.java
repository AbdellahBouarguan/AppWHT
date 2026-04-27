package com.messenger.client.media;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import java.util.function.Consumer;

public class MediaCapture {
    private OpenCVFrameGrabber grabber;
    private boolean isCapturing;

    public MediaCapture() {
        // Device 0 is default camera
        grabber = new OpenCVFrameGrabber(0);
    }

    public void startCapture(Consumer<Frame> frameConsumer) {
        try {
            grabber.setImageWidth(640);
            grabber.setImageHeight(480);
            grabber.start();
            isCapturing = true;

            new Thread(() -> {
                while (isCapturing) {
                    try {
                        Frame frame = grabber.grab();
                        if (frame != null && frameConsumer != null) {
                            frameConsumer.accept(frame);
                        }
                    } catch (FrameGrabber.Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
        }
    }

    public void stopCapture() {
        isCapturing = false;
        try {
            if (grabber != null) {
                grabber.stop();
                grabber.release();
            }
        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
        }
    }
}
