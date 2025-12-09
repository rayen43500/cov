package com.example.rideshare1.Models;

import java.util.Date;

public class Conversation {
    private String otherUserId;
    private String otherUserName;
    private String lastMessage;
    private Date lastMessageTime;
    private boolean hasUnreadMessages;
    private int unreadCount;

    public Conversation() {
    }

    public Conversation(String otherUserId, String otherUserName, String lastMessage, Date lastMessageTime, boolean hasUnreadMessages, int unreadCount) {
        this.otherUserId = otherUserId;
        this.otherUserName = otherUserName;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
        this.hasUnreadMessages = hasUnreadMessages;
        this.unreadCount = unreadCount;
    }

    public String getOtherUserId() {
        return otherUserId;
    }

    public void setOtherUserId(String otherUserId) {
        this.otherUserId = otherUserId;
    }

    public String getOtherUserName() {
        return otherUserName;
    }

    public void setOtherUserName(String otherUserName) {
        this.otherUserName = otherUserName;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public Date getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(Date lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public boolean isHasUnreadMessages() {
        return hasUnreadMessages;
    }

    public void setHasUnreadMessages(boolean hasUnreadMessages) {
        this.hasUnreadMessages = hasUnreadMessages;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }
}

