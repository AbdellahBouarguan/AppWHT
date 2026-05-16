package com.messenger.client.util;

import javax.sound.sampled.*;
import java.io.*;

public class AudioRecorder {
    private AudioFormat format;
    private TargetDataLine line;
    private ByteArrayOutputStream out;
    private volatile boolean isRecording = false;

    public AudioRecorder() {
        // 16kHz, 16bit, mono, signed, little-endian - good enough for voice
        format = new AudioFormat(16000, 16, 1, true, false);
    }

    public void start() throws LineUnavailableException {
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        if (!AudioSystem.isLineSupported(info)) {
            throw new LineUnavailableException("Microphone not supported");
        }
        line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();

        out = new ByteArrayOutputStream();
        isRecording = true;

        new Thread(() -> {
            byte[] buffer = new byte[4096];
            while (isRecording) {
                int count = line.read(buffer, 0, buffer.length);
                if (count > 0) {
                    out.write(buffer, 0, count);
                }
            }
        }).start();
    }

    public byte[] stop() {
        isRecording = false;
        if (line != null) {
            line.stop();
            line.close();
        }
        
        byte[] audioData = out.toByteArray();
        
        // Wrap in WAV header so standard players (and JavaFX Media) can play it
        return createWavFile(audioData);
    }
    
    private byte[] createWavFile(byte[] audioData) {
        ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
        AudioInputStream ais = new AudioInputStream(bais, format, audioData.length / format.getFrameSize());
        ByteArrayOutputStream wavOut = new ByteArrayOutputStream();
        try {
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, wavOut);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return wavOut.toByteArray();
    }

    public boolean isRecording() {
        return isRecording;
    }
}
