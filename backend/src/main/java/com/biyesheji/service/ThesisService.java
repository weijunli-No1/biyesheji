package com.biyesheji.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.biyesheji.common.Result;
import com.biyesheji.dto.ThesisVO;
import com.biyesheji.entity.Proposal;
import com.biyesheji.entity.ThesisVersion;
import com.biyesheji.entity.TopicSelection;
import com.biyesheji.mapper.ProposalMapper;
import com.biyesheji.mapper.ThesisVersionMapper;
import com.biyesheji.mapper.TopicSelectionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ThesisService {

    private final ThesisVersionMapper thesisVersionMapper;
    private final TopicSelectionMapper selectionMapper;
    private final ProposalMapper proposalMapper;

    /**
     * 学生上传论文版本
     * 前置条件：
     *   1. selectionId 必须属于该学生，且选题已确认(status=1)
     *   2. 开题报告必须已通过(status=3)
     *   3. 上一版论文状态不能是"待审阅(0)"（导师还没审完，不能抢先提交新版）
     */
    public Result<ThesisVersion> uploadThesis(ThesisVersion thesis, Long studentId) {
        // 验证 selectionId 归属
        TopicSelection selection = selectionMapper.selectById(thesis.getSelectionId());
        if (selection == null || !selection.getStudentId().equals(studentId)) {
            return Result.fail(403, "无权操作此选题");
        }
        if (selection.getStatus() != 1) {
            return Result.fail(400, "选题尚未确认，无法上传论文");
        }

        // 验证开题报告已通过
        Proposal proposal = proposalMapper.selectOne(new LambdaQueryWrapper<Proposal>()
                .eq(Proposal::getSelectionId, thesis.getSelectionId()));
        if (proposal == null || proposal.getStatus() != 3) {
            return Result.fail(400, "开题报告尚未通过，无法上传论文");
        }

        // 检查上一版是否还在待审阅中（未处理完毕不允许提交新版）
        ThesisVersion lastVersion = thesisVersionMapper.selectOne(new LambdaQueryWrapper<ThesisVersion>()
                .eq(ThesisVersion::getSelectionId, thesis.getSelectionId())
                .orderByDesc(ThesisVersion::getVersion)
                .last("LIMIT 1"));
        if (lastVersion != null && lastVersion.getStatus() == 0) {
            return Result.fail(400, "上一版论文导师尚未审阅完毕，请等待审阅结果后再提交新版本");
        }

        thesis.setStudentId(studentId);
        thesis.setTeacherId(selection.getTeacherId());
        thesis.setStatus(0); // 待审阅

        long count = thesisVersionMapper.selectCount(new LambdaQueryWrapper<ThesisVersion>()
                .eq(ThesisVersion::getSelectionId, thesis.getSelectionId()));
        thesis.setVersion((int) count + 1);

        thesisVersionMapper.insert(thesis);
        return Result.ok(thesis);
    }

    /**
     * 导师审阅论文
     * 只有"待审阅(0)"状态才可操作
     * 导师只能审阅自己指导的论文
     */
    public Result<?> reviewThesis(Long thesisId, Long teacherId, boolean pass, String comment) {
        ThesisVersion thesis = thesisVersionMapper.selectById(thesisId);
        if (thesis == null) return Result.fail("论文版本不存在");
        if (!thesis.getTeacherId().equals(teacherId)) return Result.fail(403, "无权审阅此论文");
        if (thesis.getStatus() != 0) return Result.fail(400, "只有待审阅状态的论文可以操作");
        if (!pass && (comment == null || comment.isBlank())) {
            return Result.fail(400, "退回时必须填写修改意见");
        }

        thesis.setComment(comment);
        thesis.setStatus(pass ? 2 : 1); // 2=已通过，1=已退回
        thesisVersionMapper.updateById(thesis);
        return Result.ok();
    }

    /** 获取论文版本历史（按版本号倒序） */
    public Result<List<ThesisVersion>> listVersions(Long selectionId) {
        var list = thesisVersionMapper.selectList(new LambdaQueryWrapper<ThesisVersion>()
                .eq(ThesisVersion::getSelectionId, selectionId)
                .orderByDesc(ThesisVersion::getVersion));
        return Result.ok(list);
    }

    /** 导师查看所有指导学生的论文列表 */
    public Result<List<ThesisVO>> listByTeacher(Long teacherId) {
        return Result.ok(thesisVersionMapper.selectByTeacher(teacherId));
    }

    /** 获取最新版本 */
    public Result<ThesisVersion> getLatest(Long selectionId) {
        var thesis = thesisVersionMapper.selectOne(new LambdaQueryWrapper<ThesisVersion>()
                .eq(ThesisVersion::getSelectionId, selectionId)
                .orderByDesc(ThesisVersion::getVersion)
                .last("LIMIT 1"));
        return Result.ok(thesis);
    }
}
