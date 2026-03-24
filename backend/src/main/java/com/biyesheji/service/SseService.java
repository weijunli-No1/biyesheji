package com.biyesheji.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE 实时推送服务
 * 维护每个在线用户的 SseEmitter 连接，当有新通知时推送给客户端。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SseService {

    private final ObjectMapper objectMapper;

    /** userId → SseEmitter（每用户只保留最新连接） */
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * 客户端订阅 SSE 流
     */
    public SseEmitter subscribe(Long userId) {
        // 超时设置为 30 分钟，前端需定时重连
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        emitters.put(userId, emitter);

        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> {
            emitters.remove(userId);
            emitter.complete();
        });
        emitter.onError(e -> emitters.remove(userId));

        // 发送连接成功心跳
        try {
            emitter.send(SseEmitter.event().name("connected").data("{\"status\":\"ok\"}"));
        } catch (IOException e) {
            emitters.remove(userId);
        }
        return emitter;
    }

    /**
     * 向指定用户推送事件（若用户离线则静默忽略）
     */
    public void push(Long userId, String eventName, Object payload) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) return;
        try {
            String json = objectMapper.writeValueAsString(payload);
            emitter.send(SseEmitter.event().name(eventName).data(json));
        } catch (IOException e) {
            log.debug("SSE push failed for user {}: {}", userId, e.getMessage());
            emitters.remove(userId);
        }
    }

    /**
     * 推送通知事件
     */
    public void pushNotification(Long userId, Object notification) {
        push(userId, "notification", notification);
    }

    /** 当前在线用户数 */
    public int onlineCount() {
        return emitters.size();
    }
}
