package com.biyesheji.controller;

import com.biyesheji.common.Result;
import com.biyesheji.entity.ClassGroup;
import com.biyesheji.entity.College;
import com.biyesheji.entity.Major;
import com.biyesheji.security.UserDetailsImpl;
import com.biyesheji.service.OrgService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/org")
@RequiredArgsConstructor
@Tag(name = "组织架构管理")
public class OrgController {

    private final OrgService orgService;

    // ==================== 学院 ====================

    @GetMapping("/colleges")
    @Operation(summary = "学院列表（全部人可见，用于下拉选）")
    public Result<?> listColleges() {
        return orgService.listColleges();
    }

    @PostMapping("/colleges")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "新建学院")
    public Result<?> createCollege(@RequestBody College college) {
        return orgService.createCollege(college);
    }

    @PutMapping("/colleges/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "修改学院")
    public Result<?> updateCollege(@PathVariable Long id, @RequestBody College college) {
        return orgService.updateCollege(id, college);
    }

    @DeleteMapping("/colleges/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "删除学院")
    public Result<?> deleteCollege(@PathVariable Long id) {
        return orgService.deleteCollege(id);
    }

    // ==================== 专业 ====================

    @GetMapping("/majors")
    @Operation(summary = "专业列表（可按学院过滤）")
    public Result<?> listMajors(@RequestParam(required = false) Long collegeId,
                                @AuthenticationPrincipal UserDetailsImpl user) {
        // 学院管理员只能看本院专业
        int role = user.getUser().getRole();
        Long scopeCollegeId = collegeId;
        if (role == 6) scopeCollegeId = user.getUser().getCollegeId();
        return orgService.listMajors(scopeCollegeId);
    }

    @PostMapping("/majors")
    @PreAuthorize("hasAnyRole('COLLEGE_ADMIN','ADMIN')")
    @Operation(summary = "新建专业")
    public Result<?> createMajor(@RequestBody Major major,
                                 @AuthenticationPrincipal UserDetailsImpl user) {
        // 学院管理员只能在本院建专业
        if (user.getUser().getRole() == 6) {
            major.setCollegeId(user.getUser().getCollegeId());
        }
        return orgService.createMajor(major);
    }

    @PutMapping("/majors/{id}")
    @PreAuthorize("hasAnyRole('COLLEGE_ADMIN','ADMIN')")
    @Operation(summary = "修改专业")
    public Result<?> updateMajor(@PathVariable Long id, @RequestBody Major major) {
        return orgService.updateMajor(id, major);
    }

    @DeleteMapping("/majors/{id}")
    @PreAuthorize("hasAnyRole('COLLEGE_ADMIN','ADMIN')")
    @Operation(summary = "删除专业")
    public Result<?> deleteMajor(@PathVariable Long id) {
        return orgService.deleteMajor(id);
    }

    // ==================== 班级 ====================

    @GetMapping("/classes")
    @Operation(summary = "班级列表（可按专业或学院过滤）")
    public Result<?> listClasses(@RequestParam(required = false) Long majorId,
                                 @RequestParam(required = false) Long collegeId,
                                 @AuthenticationPrincipal UserDetailsImpl user) {
        int role = user.getUser().getRole();
        Long scopeCollegeId = collegeId;
        Long scopeMajorId   = majorId;
        if (role == 6) scopeCollegeId = user.getUser().getCollegeId();
        if (role == 5) scopeMajorId   = user.getUser().getMajorId();
        return orgService.listClasses(scopeMajorId, scopeCollegeId);
    }

    @PostMapping("/classes")
    @PreAuthorize("hasAnyRole('MAJOR_ADMIN','COLLEGE_ADMIN','ADMIN')")
    @Operation(summary = "新建班级")
    public Result<?> createClass(@RequestBody ClassGroup classGroup,
                                 @AuthenticationPrincipal UserDetailsImpl user) {
        int role = user.getUser().getRole();
        if (role == 5) classGroup.setMajorId(user.getUser().getMajorId());
        if (role == 6) classGroup.setCollegeId(user.getUser().getCollegeId());
        return orgService.createClass(classGroup);
    }

    @PutMapping("/classes/{id}")
    @PreAuthorize("hasAnyRole('MAJOR_ADMIN','COLLEGE_ADMIN','ADMIN')")
    @Operation(summary = "修改班级")
    public Result<?> updateClass(@PathVariable Long id, @RequestBody ClassGroup classGroup) {
        return orgService.updateClass(id, classGroup);
    }

    @DeleteMapping("/classes/{id}")
    @PreAuthorize("hasAnyRole('MAJOR_ADMIN','COLLEGE_ADMIN','ADMIN')")
    @Operation(summary = "删除班级")
    public Result<?> deleteClass(@PathVariable Long id) {
        return orgService.deleteClass(id);
    }
}
