package com.logistics.service;

import lombok.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.logistics.session.*;

@Getter
@Setter
@Service
public class SessionService {

    private final Map<Long, UserSession> sessions = new ConcurrentHashMap<>();
    private ScheduledExecutorService scheduler;

    @Value("${session.timeout.minutes:30}")
    private int sessionTimeoutMinutes;

    @Value("${session.cleanup.interval.minutes:5}")
    private int cleanupIntervalMinutes;

    @PostConstruct
    public void init() {
        // Запускаем очистку просроченных сессий
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(
                this::cleanupExpiredSessions,
                cleanupIntervalMinutes,
                cleanupIntervalMinutes,
                TimeUnit.MINUTES
        );
    }

    public UserSession createSession(Long chatId) {
        UserSession session = new UserSession(chatId);
        sessions.put(chatId, session);
        return session;
    }

    public UserSession getSession(Long chatId) {
        UserSession session = sessions.get(chatId);
        if (session != null) {
            session.updateLastAccessed();
        }
        return session;
    }

    public void invalidateSession(Long chatId) {
        sessions.remove(chatId);
    }

    public boolean isSessionActive(Long chatId) {
        UserSession session = sessions.get(chatId);
        return session != null && !session.isExpired(sessionTimeoutMinutes);
    }

    public void updateSessionState(Long chatId, String state) {
        UserSession session = getSession(chatId);
        if (session != null) {
            session.setCurrentState(state);
        }
    }

    private void cleanupExpiredSessions() {
        sessions.entrySet().removeIf(entry ->
                entry.getValue().isExpired(sessionTimeoutMinutes)
        );
    }

    @PreDestroy
    public void cleanup() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }
}