package com.biyesheji.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.biyesheji.common.Result;
import com.biyesheji.dto.MidCheckVO;
import com.biyesheji.entity.MidCheck;
import com.biyesheji.entity.TopicSelection;
import com.biyesheji.mapper.MidCheckMapper;
import com.biyesheji.mapper.TopicSelectionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MidCheckService {

    private final MidCheckMapper midCheckMapper;
    private final TopicSelectionMapper selectionMapper;

    /**
     * 学生保存草稿或提交中期检查
     * 前置条件：selectionId 属于当前学生
     * 可修改状态：草稿(0) 或 已退回(3)
     */
    public Result<MidCheck> save(MidCheck midCheck, Long studentId, boolean submit) {
        TopicSelection selection = selectionMapper.selectById(midCheck.getSelectionId());
        if (selection == null || !selection.getStudentId().equals(studentId)) {
            return Result.fail(403, "无权操作此选题");
        }

        MidCheck existing = midCheckMapper.selectOne(new LambdaQueryWrapper<MidCheck>()
                .eq(MidCheck::getSelectionId, midCheck.getSelectionId()));

        if (existing != null) {
            if (existing.getStatus() != 0 && existing.getStatus() != 3) {
                return Result.fail(400, "当前状态不允许修改（审核中或已通过）");
            }
            midCheck.setId(existing.getId());
        }

        midCheck.setStudentId(studentId);
        midCheck.setTeacherId(selection.getTeacherId());

        if (submit) {
            midCheck.setStatus(1); // 待导师审核
            midCheck.setSubmitTime(LocalDateTime.now());
        } else {
            midCheck.setStatus(existing == null ? 0 : (existing.getStatus() == 3 ? 3 : 0));
        }

        if (midCheck.getId() == null) {
            midCheckMapper.insert(midCheck);
        } else {
            midCheckMapper.updateById(midCheck);
        }
        return Result.ok(midCheck);
    }

    /**
     * 导师审核中期检查
     * 只有"待导师审核(1)"状态才可操作，且只能审核自己指导的学生
     */
    public Result<?> review(Long id, Long teacherId, boolean pass, Integer progress, String comment) {
        MidCheck check = midCheckMapper.selectById(id);
        if (check == null) return Result.fail("中期检查记录不存在");
        if (!check.getTeacherId().equals(teacherId)) return Result.fail(403, "无权审核此中期检查");
        if (check.getStatus() != 1) return Result.fail(400, "只有待审核状态的记录可以操作");
        if (!pass && (comment == null || comment.isBlank())) {
            return Result.fail(400, "退回时必须填写意见");
        }

        check.setStatus(pass ? 2 : 3);
        if (pass && progress != null) check.setProgress(progress);
        check.setTeacherComment(comment);
        midCheckMapper.updateById(check);
        return Result.ok();
    }

    /** 按选题ID获取中期检查 */
    public Result<MidCheck> getBySelectionId(Long selectionId) {
        MidCheck check = midCheckMapper.selectOne(new LambdaQueryWrapper<MidCheck>()
                .eq(MidCheck::getSelectionId, selectionId));
        return Result.ok(check);
    }

    /** 列表查询（教师只看自己的，管理员可看全部） */
    public Result<List<MidCheckVO>> list(Long teacherId, Integer status) {
        return Result.ok(midCheckMapper.selectList(teacherId, status));
    }
}
