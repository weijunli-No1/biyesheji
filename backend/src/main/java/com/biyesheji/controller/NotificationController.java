package com.biyesheji.controller;

import com.biyesheji.common.Result;
import com.biyesheji.security.UserDetailsImpl;
import com.biyesheji.service.NotificationService;
import com.biyesheji.service.SseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "通知管理")
public class NotificationController {

    private final NotificationService notificationService;
    private final SseService sseService;

    /**
     * SSE 长连接订阅（前端 EventSource 连接此接口）
     * 浏览器断开自动重连，timeout 30分钟
     */
    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "SSE 实时推送订阅")
    public SseEmitter subscribe(@AuthenticationPrincipal UserDetailsImpl user) {
        return sseService.subscribe(user.getUser().getId());
    }

    @GetMapping
    @Operation(summary = "通知列表（分页）")
    public Result<?> list(@RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "20") int size,
                          @AuthenticationPrincipal UserDetailsImpl user) {
        return notificationService.list(user.getUser().getId(), page, size);
    }

    @GetMapping("/unread-count")
    @Operation(summary = "未读数量")
    public Result<?> unreadCount(@AuthenticationPrincipal UserDetailsImpl user) {
        return notificationService.unreadCount(user.getUser().getId());
    }

    @GetMapping("/recent")
    @Operation(summary = "最近10条未读通知")
    public Result<?> recent(@AuthenticationPrincipal UserDetailsImpl user) {
        return notificationService.recent(user.getUser().getId());
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "标记单条已读")
    public Result<?> markRead(@PathVariable Long id,
                              @AuthenticationPrincipal UserDetailsImpl user) {
        return notificationService.markRead(id, user.getUser().getId());
    }

    @PostMapping("/read-all")
    @Operation(summary = "全部标记已读")
    public Result<?> markAllRead(@AuthenticationPrincipal UserDetailsImpl user) {
        return notificationService.markAllRead(user.getUser().getId());
    }
}
