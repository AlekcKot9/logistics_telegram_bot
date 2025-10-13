package com.logistics.session;

import lombok.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class UserSession {
    private Long chatId;
    private String currentState;
    private Map<String, Object> attributes;
    private LocalDateTime createdAt;
    private LocalDateTime lastAccessed;

    public UserSession(Long chatId) {
        this.chatId = chatId;
        this.attributes = new HashMap<>();
        this.createdAt = LocalDateTime.now();
        this.lastAccessed = LocalDateTime.now();
        this.currentState = "STARTED";
    }

    public void removeAttribute(String key) {
        attributes.remove(key);
    }

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public void updateLastAccessed() {
        this.lastAccessed = LocalDateTime.now();
    }

    public boolean isExpired(int timeoutMinutes) {
        return lastAccessed.plusMinutes(timeoutMinutes).isBefore(LocalDateTime.now());
    }
}