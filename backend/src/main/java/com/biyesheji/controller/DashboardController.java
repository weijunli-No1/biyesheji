package com.biyesheji.controller;

import com.biyesheji.common.Result;
import com.biyesheji.security.UserDetailsImpl;
import com.biyesheji.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "工作台统计")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @Operation(summary = "工作台统计（按角色返回不同数据）")
    public Result<?> stats(@AuthenticationPrincipal UserDetailsImpl user) {
        int role = user.getUser().getRole();
        return switch (role) {
            case 1 -> dashboardService.studentStats(user.getUser().getId());
            case 2 -> dashboardService.teacherStats(user.getUser().getId());
            default -> dashboardService.adminStats();
        };
    }
}
