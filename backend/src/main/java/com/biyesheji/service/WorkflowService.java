package com.biyesheji.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.biyesheji.common.Result;
import com.biyesheji.entity.WorkflowConfig;
import com.biyesheji.mapper.WorkflowConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final WorkflowConfigMapper workflowConfigMapper;

    /**
     * 按优先级解析适用于指定学院/专业的工作流配置列表：
     * 1. 专业级配置（majorId != 0）
     * 2. 学院级配置（collegeId != 0, majorId = 0）
     * 3. 全局配置（collegeId = 0, majorId = 0）
     */
    public Result<List<WorkflowConfig>> listForScope(Integer year, Long collegeId, Long majorId) {
        // 尝试专业级
        if (majorId != null && majorId != 0) {
            List<WorkflowConfig> list = workflowConfigMapper.selectList(
                    new LambdaQueryWrapper<WorkflowConfig>()
                            .eq(WorkflowConfig::getYear, year)
                            .eq(WorkflowConfig::getMajorId, majorId));
            if (!list.isEmpty()) return Result.ok(list);
        }
        // 尝试学院级
        if (collegeId != null && collegeId != 0) {
            List<WorkflowConfig> list = workflowConfigMapper.selectList(
                    new LambdaQueryWrapper<WorkflowConfig>()
                            .eq(WorkflowConfig::getYear, year)
                            .eq(WorkflowConfig::getCollegeId, collegeId)
                            .eq(WorkflowConfig::getMajorId, 0));
            if (!list.isEmpty()) return Result.ok(list);
        }
        // 全局配置
        List<WorkflowConfig> list = workflowConfigMapper.selectList(
                new LambdaQueryWrapper<WorkflowConfig>()
                        .eq(WorkflowConfig::getYear, year)
                        .eq(WorkflowConfig::getCollegeId, 0)
                        .eq(WorkflowConfig::getMajorId, 0));
        return Result.ok(list);
    }

    /**
     * 解析当前阶段（同样按优先级查找）
     */
    public Result<?> currentStageForScope(Integer year, Long collegeId, Long majorId) {
        LocalDateTime now = LocalDateTime.now();
        // 尝试专业级
        if (majorId != null && majorId != 0) {
            WorkflowConfig stage = findCurrentStage(year, collegeId, majorId, now);
            if (stage != null) return Result.ok(stage);
        }
        // 尝试学院级
        if (collegeId != null && collegeId != 0) {
            WorkflowConfig stage = findCurrentStage(year, collegeId, 0L, now);
            if (stage != null) return Result.ok(stage);
        }
        // 全局
        return Result.ok(findCurrentStage(year, 0L, 0L, now));
    }

    private WorkflowConfig findCurrentStage(Integer year, Long collegeId, Long majorId, LocalDateTime now) {
        return workflowConfigMapper.selectOne(new LambdaQueryWrapper<WorkflowConfig>()
                .eq(WorkflowConfig::getYear, year)
                .eq(WorkflowConfig::getCollegeId, collegeId)
                .eq(WorkflowConfig::getMajorId, majorId)
                .le(WorkflowConfig::getStartTime, now)
                .ge(WorkflowConfig::getEndTime, now)
                .last("LIMIT 1"));
    }

    /**
     * 管理员保存（批量 upsert），按 stage+year+collegeId+majorId 唯一键更新
     */
    public Result<?> batchSave(List<WorkflowConfig> configs) {
        configs.forEach(c -> {
            var existing = workflowConfigMapper.selectOne(new LambdaQueryWrapper<WorkflowConfig>()
                    .eq(WorkflowConfig::getStage, c.getStage())
                    .eq(WorkflowConfig::getYear, c.getYear())
                    .eq(WorkflowConfig::getCollegeId, c.getCollegeId() != null ? c.getCollegeId() : 0)
                    .eq(WorkflowConfig::getMajorId,   c.getMajorId()   != null ? c.getMajorId()   : 0));
            if (c.getCollegeId() == null) c.setCollegeId(0L);
            if (c.getMajorId()   == null) c.setMajorId(0L);
            if (existing != null) {
                c.setId(existing.getId());
                workflowConfigMapper.updateById(c);
            } else {
                workflowConfigMapper.insert(c);
            }
        });
        return Result.ok();
    }
}
