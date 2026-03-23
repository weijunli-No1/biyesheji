package com.biyesheji.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.biyesheji.common.PageResult;
import com.biyesheji.common.Result;
import com.biyesheji.dto.ProposalVO;
import com.biyesheji.entity.Proposal;
import com.biyesheji.entity.TopicSelection;
import com.biyesheji.mapper.ProposalMapper;
import com.biyesheji.mapper.TopicSelectionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProposalService {

    private final ProposalMapper proposalMapper;
    private final TopicSelectionMapper selectionMapper;

    /**
     * 学生保存/提交开题报告
     * 前置条件：
     *   1. selectionId 必须属于该学生
     *   2. 选题状态必须为"已确认(1)"才能提交
     *   3. 开题报告状态为"草稿(0)"或"已退回(4)"才允许修改
     */
    public Result<Proposal> saveProposal(Proposal proposal, Long studentId, boolean submit) {
        // 验证 selectionId 归属
        TopicSelection selection = selectionMapper.selectById(proposal.getSelectionId());
        if (selection == null || !selection.getStudentId().equals(studentId)) {
            return Result.fail(403, "无权操作此选题");
        }
        // 提交时要求选题已确认
        if (submit && selection.getStatus() != 1) {
            return Result.fail(400, "选题尚未被导师确认，无法提交开题报告");
        }

        // 检查是否已有开题报告
        var existing = proposalMapper.selectOne(new LambdaQueryWrapper<Proposal>()
                .eq(Proposal::getSelectionId, proposal.getSelectionId()));

        if (existing != null) {
            // 只有草稿(0)或已退回(4)状态才允许编辑
            if (existing.getStatus() != 0 && existing.getStatus() != 4) {
                return Result.fail(400, "当前状态不允许修改（审核中或已通过）");
            }
            proposal.setId(existing.getId());
        }

        proposal.setStudentId(studentId);
        proposal.setTeacherId(selection.getTeacherId());

        if (submit) {
            proposal.setStatus(1); // 待导师审核
            proposal.setSubmitTime(LocalDateTime.now());
        } else {
            proposal.setStatus(existing == null ? 0 : existing.getStatus() == 4 ? 4 : 0);
        }

        if (proposal.getId() == null) {
            proposalMapper.insert(proposal);
        } else {
            proposalMapper.updateById(proposal);
        }
        return Result.ok(proposal);
    }

    /**
     * 导师审核开题报告
     * 只有"待导师审核(1)"状态才可操作
     * 导师只能审核自己指导的学生的开题报告
     */
    public Result<?> teacherReview(Long proposalId, Long teacherId, boolean pass,
                                   String comment, String rejectReason) {
        Proposal proposal = proposalMapper.selectById(proposalId);
        if (proposal == null) return Result.fail("开题报告不存在");
        if (!proposal.getTeacherId().equals(teacherId)) return Result.fail(403, "无权审核此开题报告");
        if (proposal.getStatus() != 1) return Result.fail(400, "只有待导师审核状态的开题报告可以操作");
        if (!pass && (rejectReason == null || rejectReason.isBlank())) {
            return Result.fail(400, "退回时必须填写原因");
        }

        proposal.setTeacherComment(comment);
        if (pass) {
            proposal.setStatus(2); // 待评审
            proposal.setRejectReason(null);
        } else {
            proposal.setStatus(4); // 已退回
            proposal.setRejectReason(rejectReason);
        }
        proposalMapper.updateById(proposal);
        return Result.ok();
    }

    /**
     * 评审组审核（院系管理员）
     * 只有"待评审(2)"状态才可操作
     */
    public Result<?> deptReview(Long proposalId, boolean pass, String reviewComment,
                                Integer reviewScore, String rejectReason) {
        Proposal proposal = proposalMapper.selectById(proposalId);
        if (proposal == null) return Result.fail("开题报告不存在");
        if (proposal.getStatus() != 2) return Result.fail(400, "只有待评审状态的开题报告可以操作");
        if (!pass && (rejectReason == null || rejectReason.isBlank())) {
            return Result.fail(400, "退回时必须填写原因");
        }
        if (pass && (reviewScore == null || reviewScore < 0 || reviewScore > 100)) {
            return Result.fail(400, "请填写有效的评审分数（0-100）");
        }

        proposal.setReviewComment(reviewComment);
        proposal.setReviewScore(reviewScore);
        if (pass) {
            proposal.setStatus(3); // 已通过
            proposal.setRejectReason(null);
        } else {
            proposal.setStatus(4); // 已退回，学生需修改后重新提交
            proposal.setRejectReason(rejectReason);
        }
        proposalMapper.updateById(proposal);
        return Result.ok();
    }

    public Result<Proposal> getBySelectionId(Long selectionId) {
        var proposal = proposalMapper.selectOne(new LambdaQueryWrapper<Proposal>()
                .eq(Proposal::getSelectionId, selectionId));
        return Result.ok(proposal);
    }

    public Result<PageResult<ProposalVO>> listProposals(int page, int size, Long teacherId, Integer status) {
        Page<ProposalVO> p = proposalMapper.selectProposalList(new Page<>(page, size), teacherId, status);
        return Result.ok(PageResult.of(p));
    }
}
