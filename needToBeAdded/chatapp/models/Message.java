package com.chatapp.models;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Message {
    private String sender;
    private String content;
    private String time;
    private boolean isMe;

    public Message(String sender, String content, boolean isMe) {
        this.sender = sender;
        this.content = content;
        this.isMe = isMe;
        this.time = LocalTime.now()
                .format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    public String getSender() { return sender; }
    public String getContent() { return content; }
    public String getTime() { return time; }
    public boolean isMe() { return isMe; }
}
