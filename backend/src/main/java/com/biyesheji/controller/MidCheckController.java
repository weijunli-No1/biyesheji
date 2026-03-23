package com.biyesheji.controller;

import com.biyesheji.common.Result;
import com.biyesheji.entity.MidCheck;
import com.biyesheji.security.UserDetailsImpl;
import com.biyesheji.service.MidCheckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/mid-checks")
@RequiredArgsConstructor
@Tag(name = "中期检查")
public class MidCheckController {

    private final MidCheckService midCheckService;

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "保存中期检查草稿")
    public Result<?> save(@RequestBody MidCheck midCheck,
                          @AuthenticationPrincipal UserDetailsImpl user) {
        return midCheckService.save(midCheck, user.getUser().getId(), false);
    }

    @PostMapping("/submit")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "提交中期检查")
    public Result<?> submit(@RequestBody MidCheck midCheck,
                            @AuthenticationPrincipal UserDetailsImpl user) {
        return midCheckService.save(midCheck, user.getUser().getId(), true);
    }

    @GetMapping("/selection/{selectionId}")
    @Operation(summary = "按选题ID获取中期检查")
    public Result<?> getBySelection(@PathVariable Long selectionId) {
        return midCheckService.getBySelectionId(selectionId);
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "导师审核中期检查")
    public Result<?> review(@PathVariable Long id,
                            @RequestBody Map<String, Object> body,
                            @AuthenticationPrincipal UserDetailsImpl user) {
        boolean pass = Boolean.TRUE.equals(body.get("pass"));
        Integer progress = body.get("progress") instanceof Number n ? n.intValue() : null;
        String comment = (String) body.get("comment");
        return midCheckService.review(id, user.getUser().getId(), pass, progress, comment);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER','MAJOR_ADMIN','COLLEGE_ADMIN','ADMIN')")
    @Operation(summary = "中期检查列表（教师看自己的，管理员看全部）")
    public Result<?> list(@RequestParam(required = false) Integer status,
                          @AuthenticationPrincipal UserDetailsImpl user) {
        Long teacherId = user.getUser().getRole() == 2 ? user.getUser().getId() : null;
        return midCheckService.list(teacherId, status);
    }
}
