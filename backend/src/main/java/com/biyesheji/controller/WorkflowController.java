package com.biyesheji.controller;

import com.biyesheji.common.Result;
import com.biyesheji.entity.WorkflowConfig;
import com.biyesheji.mapper.WorkflowConfigMapper;
import com.biyesheji.security.UserDetailsImpl;
import com.biyesheji.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/workflow")
@RequiredArgsConstructor
@Tag(name = "工作流配置")
public class WorkflowController {

    private final WorkflowConfigMapper workflowConfigMapper;
    private final WorkflowService workflowService;

    /**
     * 获取工作流时间节点列表（按用户身份解析作用域：专业>学院>全局）
     */
    @GetMapping
    @Operation(summary = "获取工作流时间节点（自动按作用域解析）")
    public Result<?> list(@RequestParam Integer year,
                          @AuthenticationPrincipal UserDetailsImpl user) {
        Long collegeId = user != null ? user.getUser().getCollegeId() : null;
        Long majorId   = user != null ? user.getUser().getMajorId()   : null;
        return workflowService.listForScope(year, collegeId, majorId);
    }

    /**
     * 获取当前阶段（按用户身份解析作用域）
     */
    @GetMapping("/current-stage")
    @Operation(summary = "获取当前阶段（自动按作用域解析）")
    public Result<?> currentStage(@RequestParam Integer year,
                                  @AuthenticationPrincipal UserDetailsImpl user) {
        Long collegeId = user != null ? user.getUser().getCollegeId() : null;
        Long majorId   = user != null ? user.getUser().getMajorId()   : null;
        return workflowService.currentStageForScope(year, collegeId, majorId);
    }

    /**
     * 修改单条时间节点（管理员）
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MAJOR_ADMIN','COLLEGE_ADMIN','ADMIN')")
    @Operation(summary = "更新时间节点（管理员）")
    public Result<?> update(@PathVariable Long id, @RequestBody WorkflowConfig config,
                            @AuthenticationPrincipal UserDetailsImpl user) {
        config.setId(id);
        // 确保管理员只能修改自己作用域的配置
        int role = user.getUser().getRole();
        if (role == 5) { // MAJOR_ADMIN
            config.setMajorId(user.getUser().getMajorId());
            config.setCollegeId(user.getUser().getCollegeId());
        } else if (role == 6) { // COLLEGE_ADMIN
            config.setCollegeId(user.getUser().getCollegeId());
            config.setMajorId(0L);
        }
        workflowConfigMapper.updateById(config);
        return Result.ok();
    }

    /**
     * 批量保存时间节点（管理员，自动按角色设置 collegeId/majorId）
     */
    @PostMapping("/batch")
    @PreAuthorize("hasAnyRole('MAJOR_ADMIN','COLLEGE_ADMIN','ADMIN')")
    @Operation(summary = "批量保存时间节点")
    public Result<?> batchSave(@RequestBody List<WorkflowConfig> configs,
                               @AuthenticationPrincipal UserDetailsImpl user) {
        int role = user.getUser().getRole();
        configs.forEach(c -> {
            if (role == 5) { // MAJOR_ADMIN
                c.setMajorId(user.getUser().getMajorId());
                c.setCollegeId(user.getUser().getCollegeId());
            } else if (role == 6) { // COLLEGE_ADMIN
                c.setCollegeId(user.getUser().getCollegeId());
                c.setMajorId(0L);
            } else { // ADMIN — 全局配置
                if (c.getCollegeId() == null) c.setCollegeId(0L);
                if (c.getMajorId()   == null) c.setMajorId(0L);
            }
        });
        return workflowService.batchSave(configs);
    }
}
