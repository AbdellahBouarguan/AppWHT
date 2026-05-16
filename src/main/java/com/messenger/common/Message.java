package com.messenger.common;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum MessageStatus {
        SENDING,
        SENT_TO_SERVER,
        DELIVERED_TO_CLIENT,
        READ
    }

    private String id;
    private String content;
    private User sender;
    private User receiver;
    private LocalDateTime timestamp;
    private MessageStatus status;
    private byte[] fileData;
    private String fileName;
    private String parentMessageId;
    private String parentMessageContent;
    private java.util.Map<String, String> reactions = new java.util.HashMap<>(); // userId -> emoji
    private String linkTitle;
    private String linkDescription;
    private String linkImageUrl;

    public Message(User sender, User receiver, String content) {
        this.id = UUID.randomUUID().toString();
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.timestamp = LocalDateTime.now();
        this.status = MessageStatus.SENDING;
    }

    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public User getReceiver() {
        return receiver;
    }

    public void setReceiver(User receiver) {
        this.receiver = receiver;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public MessageStatus getStatus() {
        return status;
    }

    public void setStatus(MessageStatus status) {
        this.status = status;
    }

    public byte[] getFileData() {
        return fileData;
    }

    public void setFileData(byte[] fileData) {
        this.fileData = fileData;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getParentMessageId() {
        return parentMessageId;
    }

    public void setParentMessageId(String parentMessageId) {
        this.parentMessageId = parentMessageId;
    }

    public String getParentMessageContent() {
        return parentMessageContent;
    }

    public void setParentMessageContent(String parentMessageContent) {
        this.parentMessageContent = parentMessageContent;
    }

    public java.util.Map<String, String> getReactions() {
        return reactions;
    }

    public void setReactions(java.util.Map<String, String> reactions) {
        this.reactions = reactions;
    }

    public String getLinkTitle() { return linkTitle; }
    public void setLinkTitle(String linkTitle) { this.linkTitle = linkTitle; }
    
    public String getLinkDescription() { return linkDescription; }
    public void setLinkDescription(String linkDescription) { this.linkDescription = linkDescription; }
    
    public String getLinkImageUrl() { return linkImageUrl; }
    public void setLinkImageUrl(String linkImageUrl) { this.linkImageUrl = linkImageUrl; }

    public void clearFileData() {
        this.fileData = null;
    }
}
