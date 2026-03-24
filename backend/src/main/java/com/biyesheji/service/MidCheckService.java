package com.biyesheji.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.biyesheji.common.Result;
import com.biyesheji.dto.MidCheckVO;
import com.biyesheji.entity.MidCheck;
import com.biyesheji.entity.Proposal;
import com.biyesheji.entity.TopicSelection;
import com.biyesheji.entity.User;
import com.biyesheji.mapper.MidCheckMapper;
import com.biyesheji.mapper.ProposalMapper;
import com.biyesheji.mapper.TopicSelectionMapper;
import com.biyesheji.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MidCheckService {

    private final MidCheckMapper midCheckMapper;
    private final TopicSelectionMapper selectionMapper;
    private final ProposalMapper proposalMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;

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

        // P1: 提交时强制校验开题报告已通过（后端守卫，前端已有 UX 提示但不可绕过）
        if (submit) {
            Proposal proposal = proposalMapper.selectOne(new LambdaQueryWrapper<Proposal>()
                    .eq(Proposal::getSelectionId, midCheck.getSelectionId()));
            if (proposal == null || proposal.getStatus() != 3) {
                return Result.fail(400, "开题报告尚未通过评审，无法提交中期检查");
            }
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
            // P6: 保存草稿始终置为草稿(0)，不保留"已退回"状态，使学生明确感知"编辑中"
            midCheck.setStatus(0);
        }

        if (midCheck.getId() == null) {
            midCheckMapper.insert(midCheck);
        } else {
            midCheckMapper.updateById(midCheck);
        }

        if (submit) {
            User student = userMapper.selectById(studentId);
            String studentName = student != null ? student.getRealName() : "学生";
            notificationService.midCheckSubmitted(midCheck.getTeacherId(), midCheck.getId(), studentName);
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

        if (pass) {
            notificationService.midCheckPassed(check.getStudentId(), id);
        } else {
            notificationService.midCheckRejected(check.getStudentId(), id, comment);
        }
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
        return Result.ok(midCheckMapper.selectMidCheckList(teacherId, status));
    }
}
