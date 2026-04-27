package com.messenger.common;

import java.io.Serializable;

/**
 * Wrapper for all network communications between Client and Server.
 */
public class NetworkPayload implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum PayloadType {
        AUTH_REQUEST,
        AUTH_RESPONSE,
        LOGOUT_REQUEST,
        
        MESSAGE,
        MESSAGE_ACK,
        
        CALL_REQUEST,
        CALL_RESPONSE,
        
        ADMIN_KICK,
        ADMIN_BROADCAST,
        
        USERS_LIST_UPDATE
    }

    private PayloadType type;
    private Object data;
    private String status; // e.g., "OK", "ERROR", "ACCEPTED", "REJECTED"
    
    public NetworkPayload(PayloadType type, Object data) {
        this.type = type;
        this.data = data;
    }
    
    public NetworkPayload(PayloadType type, Object data, String status) {
        this.type = type;
        this.data = data;
        this.status = status;
    }

    public PayloadType getType() {
        return type;
    }

    public Object getData() {
        return data;
    }

    public String getStatus() {
        return status;
    }
}
