package com.biyesheji.controller;

import com.biyesheji.common.Result;
import com.biyesheji.entity.DefenseGroup;
import com.biyesheji.security.UserDetailsImpl;
import com.biyesheji.service.DefenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/defense")
@RequiredArgsConstructor
@Tag(name = "答辩管理")
public class DefenseController {

    private final DefenseService defenseService;

    @GetMapping("/groups")
    @PreAuthorize("hasAnyRole('DEFENSE_MEMBER','MAJOR_ADMIN','COLLEGE_ADMIN','ADMIN')")
    @Operation(summary = "答辩小组列表")
    public Result<?> listGroups(@RequestParam(defaultValue = "2026") Integer year) {
        return defenseService.listGroups(year);
    }

    @PostMapping("/groups")
    @PreAuthorize("hasAnyRole('MAJOR_ADMIN','COLLEGE_ADMIN','ADMIN')")
    @Operation(summary = "创建答辩小组")
    public Result<?> createGroup(@RequestBody DefenseGroup group) {
        return defenseService.createGroup(group);
    }

    @GetMapping("/groups/{groupId}/students")
    @PreAuthorize("hasAnyRole('DEFENSE_MEMBER','MAJOR_ADMIN','COLLEGE_ADMIN','ADMIN')")
    @Operation(summary = "小组答辩学生列表")
    public Result<?> listGroupStudents(@PathVariable Long groupId) {
        return defenseService.listGroupStudents(groupId);
    }

    @PostMapping("/groups/{groupId}/students")
    @PreAuthorize("hasAnyRole('MAJOR_ADMIN','COLLEGE_ADMIN','ADMIN')")
    @Operation(summary = "分配学生到答辩组")
    public Result<?> assignStudent(@PathVariable Long groupId,
                                   @RequestBody Map<String, Long> body) {
        Long selectionId = body.get("selectionId");
        if (selectionId == null) return Result.fail("selectionId 不能为空");
        return defenseService.assignStudent(groupId, selectionId);
    }

    @PostMapping("/records")
    @PreAuthorize("hasAnyRole('DEFENSE_MEMBER','MAJOR_ADMIN','COLLEGE_ADMIN','ADMIN')")
    @Operation(summary = "录入答辩成绩")
    public Result<?> saveRecord(@RequestBody Map<String, Object> body) {
        if (body.get("selectionId") == null) return Result.fail("selectionId 不能为空");
        return defenseService.saveRecord(body);
    }

    @PostMapping("/records/{recordId}/confirm-revision")
    @PreAuthorize("hasAnyRole('MAJOR_ADMIN','COLLEGE_ADMIN','ADMIN')")
    @Operation(summary = "确认修改后通过学生已完成修改（解锁锁定资格）")
    public Result<?> confirmRevision(@PathVariable Long recordId) {
        return defenseService.confirmRevision(recordId);
    }

    @GetMapping("/records/my")
    @Operation(summary = "学生查看自己的答辩记录")
    public Result<?> myRecord(@AuthenticationPrincipal UserDetailsImpl user) {
        return defenseService.myRecord(user.getUser().getId());
    }

    @GetMapping("/students/unassigned")
    @PreAuthorize("hasAnyRole('MAJOR_ADMIN','COLLEGE_ADMIN','ADMIN')")
    @Operation(summary = "未分配答辩组的学生")
    public Result<?> unassignedStudents(@RequestParam(defaultValue = "2026") Integer year) {
        return defenseService.unassignedStudents(year);
    }
}
