package com.messenger.common;

import java.io.Serializable;

/**
 * Wrapper for all network communications between Client and Server.
 */
public class NetworkPayload implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum PayloadType {
        REGISTER_REQUEST,
        REGISTER_SUCCESS,
        LOGIN_REQUEST,
        LOGIN_SUCCESS,
        ADD_CONTACT_REQUEST,
        FETCH_CONTACTS_REQUEST,
        CONTACT_LIST_RESPONSE,
        FETCH_CHAT_HISTORY_REQUEST,
        FETCH_CHAT_HISTORY_RESPONSE,
        SEND_MESSAGE,
        RECEIVE_MESSAGE,
        CALL_REQUEST,
        CALL_ACCEPT,
        CALL_REJECT,
        END_CALL,
        STATUS_UPDATE,
        LOGOUT_REQUEST,
        MESSAGE_ACK,
        TYPING_UPDATE,
        UPDATE_PROFILE,
        MESSAGE_READ,
        DELETE_MESSAGE,
        MESSAGE_REACTION,
        CREATE_GROUP_REQUEST,
        CREATE_GROUP_SUCCESS,
        FETCH_GROUPS_REQUEST,
        FETCH_GROUPS_RESPONSE,
        GROUP_MESSAGE_RECEIVE,
        ADD_GROUP_MEMBER,
        GROUP_CALL_JOIN_REQUEST,
        GROUP_CALL_JOIN_SUCCESS,
        GROUP_CALL_LEAVE_REQUEST,
        GROUP_CALL_STATE_UPDATE,
        GROUP_CALL_STARTED,
        BLOCK_CONTACT_REQUEST,
        ACCEPT_CONTACT_REQUEST,
        CONTACT_STATUS_UPDATE
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
