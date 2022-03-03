package com.example.chatapp.model;

public class GroupMessage {
    String message, timestamp, sender, type;

    public GroupMessage() {
    }

    public GroupMessage(String message, String timestamp, String sender, String type) {
        this.message = message;
        this.timestamp = timestamp;
        this.sender = sender;
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "GroupMessage{" +
                "message='" + message + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", sender='" + sender + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
