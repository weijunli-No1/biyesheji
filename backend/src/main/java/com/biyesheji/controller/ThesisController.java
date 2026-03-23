package com.biyesheji.controller;

import com.biyesheji.common.Result;
import com.biyesheji.entity.ThesisVersion;
import com.biyesheji.security.UserDetailsImpl;
import com.biyesheji.service.ThesisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/thesis")
@RequiredArgsConstructor
@Tag(name = "论文管理")
public class ThesisController {

    private final ThesisService thesisService;

    @PostMapping("/upload")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "上传论文版本")
    public Result<?> upload(@RequestBody ThesisVersion thesis,
                            @AuthenticationPrincipal UserDetailsImpl user) {
        return thesisService.uploadThesis(thesis, user.getUser().getId());
    }

    @GetMapping("/versions/{selectionId}")
    @Operation(summary = "获取论文版本列表")
    public Result<?> versions(@PathVariable Long selectionId) {
        return thesisService.listVersions(selectionId);
    }

    @GetMapping("/latest/{selectionId}")
    @Operation(summary = "获取最新论文版本")
    public Result<?> latest(@PathVariable Long selectionId) {
        return thesisService.getLatest(selectionId);
    }

    @GetMapping("/teacher-list")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "导师查看所有学生论文列表")
    public Result<?> teacherList(@AuthenticationPrincipal UserDetailsImpl user) {
        return thesisService.listByTeacher(user.getUser().getId());
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "导师审阅论文")
    public Result<?> review(@PathVariable Long id,
                            @RequestBody Map<String, Object> body,
                            @AuthenticationPrincipal UserDetailsImpl user) {
        boolean pass = Boolean.TRUE.equals(body.get("pass"));
        String comment = (String) body.get("comment");
        return thesisService.reviewThesis(id, user.getUser().getId(), pass, comment);
    }
}
