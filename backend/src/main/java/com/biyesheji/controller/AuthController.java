package com.biyesheji.controller;

import com.biyesheji.common.Result;
import com.biyesheji.dto.LoginRequest;
import com.biyesheji.security.UserDetailsImpl;
import com.biyesheji.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "认证")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "登录")
    public Result<?> login(@RequestBody @Valid LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息")
    public Result<?> me(@AuthenticationPrincipal UserDetailsImpl user) {
        return authService.getCurrentUser(user.getUser().getId());
    }
}
